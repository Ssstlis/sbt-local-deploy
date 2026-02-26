import com.github.sbt.git.SbtGit.git
import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.*
import sbt.*
import sbt.Keys.*
import sbt.internal.util.ManagedLogger

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.collection.JavaConverters.*

object LocalDeployPlugin extends AutoPlugin {

  private val DEPLOY_PATH_ENV_NAME = "SDP_SCALA_APP_DEPLOY_PATH"
  private val LINK_PATH_ENV_NAME   = "SDP_SCALA_APP_DEPLOY_LINK_PATH"
  private val SHARED_DIRS          = List("logs", "conf")

  override def requires: Plugins = UniversalPlugin
  override def trigger           = noTrigger

  object autoImport {
    val deploy = inputKey[Unit](
      "Stage and deploy the distribution.\n" +
        "Usage: deploy <deployPath> <linkPath>\n" +
        s"  deployPath — root dir, default value from env $DEPLOY_PATH_ENV_NAME; distributable is placed at <deployPath>/<name>/<name>-<version>-<time>-<commit>/\n" +
        s"               a 'current' symlink at <deployPath>/<name>/current is updated to point to the new release\n" +
        s"  linkPath   — dir where bin/* scripts are symlinked, default value from env $LINK_PATH_ENV_NAME"
    )
    val deployInfo = inputKey[Unit](
      "Show where the installation would take place, where to place, where to link.\n" +
        "Usage: deployInfo <deployPath> <linkPath>\n" +
        s"  deployPath — root dir, default value from env $DEPLOY_PATH_ENV_NAME; distributable is placed at <deployPath>/<name>/<name>-<version>-<time>-<commit>/\n" +
        s"  linkPath   — dir where bin/* scripts are symlinked, default value from env $LINK_PATH_ENV_NAME"
    )
    val staleInstallations = inputKey[Unit](
      "Shows stale installations (older than 30 days).\n" +
        "Usage: staleInstallations <deployPath>\n" +
        s"  deployPath — root dir, default value from env $DEPLOY_PATH_ENV_NAME; scans <deployPath>/<name>/\n"
    )
  }

  import autoImport.*

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    deploy             := deployTaskImpl.evaluated,
    deployInfo         := deployInfoTaskImpl.evaluated,
    staleInstallations := staleInstallationsTaskImpl.evaluated
  )

  // ── helpers ──────────────────────────────────────────────────────────────

  private def formatBytes(bytes: Long): String = {
    val kb = 1024L; val mb = kb * 1024; val gb = mb * 1024
    if (bytes >= gb) f"${bytes.toDouble / gb}%.1f GB"
    else if (bytes >= mb) f"${bytes.toDouble / mb}%.1f MB"
    else if (bytes >= kb) f"${bytes.toDouble / kb}%.1f KB"
    else s"$bytes B"
  }

  private def copyDir(src: Path, dest: Path): Unit = {
    val stream = Files.walk(src)
    try
      stream.iterator().asScala.foreach { source =>
        val target = dest.resolve(src.relativize(source))
        if (Files.isDirectory(source)) Files.createDirectories(target)
        else {
          Files.createDirectories(target.getParent)
          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        }
      }
    finally stream.close()
  }

  private def dirSize(root: Path): Long = {
    val stream = Files.walk(root)
    try stream.iterator().asScala.filter(p => Files.isRegularFile(p)).map(p => Files.size(p)).sum
    finally stream.close()
  }

  private val DATE_TIME_FORMATTER = {
    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
  }

  private def getEnv(envName: String, log: ManagedLogger) = {
    sys.env.get(envName) match {
      case Some(value) => value
      case None        =>
        log.err(s"$envName env not set")
        null
    }
  }

  private case class DeployCtx(
    stageDir: Path,
    pName: String,
    pVersion: String,
    pCommit: String,
    pTime: String,
    log: ManagedLogger
  ) {
    def dirName: String = s"$pName-$pVersion-$pTime-$pCommit"
  }

  private val deployCtx: Def.Initialize[Task[DeployCtx]] = Def.task {
    DeployCtx(
      stageDir = (Universal / stage).value.toPath,
      pName = name.value,
      pVersion = version.value,
      pCommit = git.gitHeadCommit.value.getOrElse("unknown").take(8),
      pTime = DATE_TIME_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC)),
      log = streams.value.log
    )
  }

  private lazy val isWindows = System.getProperty("os.name").toLowerCase.contains("win")

  private def staleInstallationsDef(
    deployRoot: Path,
    projectName: String,
    filter: Path => Boolean,
    log: ManagedLogger
  ): Unit = {
    if (Files.exists(deployRoot) && Files.isDirectory(deployRoot)) {
      val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
      val stream        = Files.list(deployRoot)
      val stale         = try {
        stream
          .iterator()
          .asScala
          .filter(p => Files.isDirectory(p) && filter(p) && p.getFileName.toString.startsWith(projectName))
          .flatMap { p =>
            val mtime = Files.getLastModifiedTime(p).toInstant
            if (mtime.isBefore(thirtyDaysAgo))
              Some(p -> ChronoUnit.DAYS.between(mtime, Instant.now()))
            else None
          }
          .toList
          .sortBy(-_._2)
      } finally stream.close()

      if (stale.nonEmpty) {
        log.warn(s"Stale deployments in $deployRoot (older than 30 days):")
        stale.foreach { case (p, days) =>
          log.warn(s"  ${p.getFileName}  ($days days ago)")
        }
      }
    }
  }

  private def installApp(stageDir: Path, destDir: Path, log: ManagedLogger): Unit = {
    if (Files.exists(destDir)) {
      val stream = Files.walk(destDir)
      try
        stream
          .sorted(java.util.Comparator.reverseOrder[Path]())
          .iterator()
          .asScala
          .foreach(p => Files.delete(p))
      finally stream.close()
    }
    copyDir(stageDir, destDir)
    log.info(
      s"Deployed  : ${scala.Console.CYAN}$destDir${scala.Console.RESET}  (${scala.Console.GREEN}${formatBytes(dirSize(destDir))}${scala.Console.RESET})"
    )
  }

  private def symlinks(linkRoot: Path, stageBinDir: Path, destBinDir: Path, isWindows: Boolean): List[(Path, Path)] = {
    if (Files.exists(stageBinDir)) {
      val stream = Files.list(stageBinDir)
      try
        stream
          .iterator()
          .asScala
          .toList
          .flatMap { bin =>
            val fileName = bin.getFileName.toString
            val isScript = if (isWindows) fileName.endsWith(".bat") else !fileName.endsWith(".bat")
            if (isScript) Some(linkRoot.resolve(fileName) -> destBinDir.resolve(fileName))
            else None
          }
      finally stream.close()
    } else Nil
  }

  private def symlinkBinary(
    linkRoot: Path,
    stageBinDir: Path,
    destBinDir: Path,
    isWindows: Boolean,
    log: ManagedLogger
  ): Unit = {
    Files.createDirectories(linkRoot)
    symlinks(linkRoot, stageBinDir, destBinDir, isWindows).foreach { case (link, bin) =>
      if (Files.exists(link) || Files.isSymbolicLink(link)) Files.delete(link)
      Files.createSymbolicLink(link, bin)
      log.info(
        s"Linked    : ${scala.Console.CYAN}$link${scala.Console.RESET}  →  ${scala.Console.CYAN}$bin${scala.Console.RESET}"
      )
    }
  }

  private def updateCurrentSymlink(appDir: Path, destDir: Path, log: ManagedLogger): Unit = {
    val currentLink = appDir.resolve("current")
    if (Files.exists(currentLink) || Files.isSymbolicLink(currentLink)) Files.delete(currentLink)
    Files.createSymbolicLink(currentLink, destDir)
    log.info(
      s"Current   : ${scala.Console.CYAN}$currentLink${scala.Console.RESET}  →  ${scala.Console.CYAN}$destDir${scala.Console.RESET}"
    )
  }

  private def linkSharedDirs(appDir: Path, destDir: Path, log: ManagedLogger): Unit =
    SHARED_DIRS.foreach { dirName =>
      val shared = appDir.resolve(dirName)
      Files.createDirectories(shared)
      val link = destDir.resolve(dirName)
      if (Files.exists(link) || Files.isSymbolicLink(link)) Files.delete(link)
      Files.createSymbolicLink(link, shared)
      log.info(
        s"Shared    : ${scala.Console.CYAN}$link${scala.Console.RESET}  →  ${scala.Console.CYAN}$shared${scala.Console.RESET}"
      )
    }

  // ── task ─────────────────────────────────────────────────────────────────

  private lazy val deployTaskImpl: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    import sbt.complete.DefaultParsers._

    val (deployArgOpt, linkPathOpt) = ((Space.? ~> StringBasic.?) ~ (Space.? ~> StringBasic.?)).parsed

    val ctx = deployCtx.value
    import ctx.{stageDir, dirName, pName, log}

    lazy val deployRootFromEnv = getEnv(DEPLOY_PATH_ENV_NAME, log)
    lazy val linkRootFromEnv   = getEnv(LINK_PATH_ENV_NAME, log)

    val deployRoot = Paths.get(deployArgOpt.getOrElse(deployRootFromEnv))
    val linkRoot   = Paths.get(linkPathOpt.getOrElse(linkRootFromEnv))
    val appDir     = deployRoot.resolve(pName)
    val destDir    = appDir.resolve(dirName)

    // ── 1. Copy stageDir → destDir ──────────────────────────────────────────
    installApp(stageDir, destDir.resolve("bin"), log)

    // ── 2. Update 'current' symlink → destDir ───────────────────────────────
    updateCurrentSymlink(appDir, destDir, log)

    // ── 3. Link shared dirs (logs, conf) into destDir ───────────────────────
    linkSharedDirs(appDir, destDir, log)

    // ── 4. Symlink bin/* → linkRoot/ ────────────────────────────────────────
    symlinkBinary(linkRoot, stageDir.resolve("bin"), destDir.resolve("bin"), isWindows, log)

    // ── 5. Warn about stale deployments (> 30 days old) ─────────────────────
    staleInstallationsDef(appDir, pName, _ != destDir, log)
  }

  private lazy val deployInfoTaskImpl: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    import sbt.complete.DefaultParsers._

    val (deployArgOpt, linkPathOpt) = ((Space.? ~> StringBasic.?) ~ (Space.? ~> StringBasic.?)).parsed

    val ctx = deployCtx.value
    import ctx.{stageDir, log}

    lazy val deployRootFromEnv = getEnv(DEPLOY_PATH_ENV_NAME, log)
    lazy val linkRootFromEnv   = getEnv(LINK_PATH_ENV_NAME, log)

    val deployRoot  = Paths.get(deployArgOpt.getOrElse(deployRootFromEnv))
    val linkRoot    = Paths.get(linkPathOpt.getOrElse(linkRootFromEnv))
    val appDir      = deployRoot.resolve(ctx.pName)
    val destDir     = appDir.resolve(ctx.dirName)
    val currentLink = appDir.resolve("current")

    log.info(s"Stage path  : ${scala.Console.CYAN}${stageDir.toAbsolutePath}${scala.Console.RESET}")
    log.info(s"Deploy path : ${scala.Console.YELLOW}${destDir.toAbsolutePath}${scala.Console.RESET}")
    log.info(
      s"Current link: ${scala.Console.YELLOW}$currentLink${scala.Console.RESET}  →  ${scala.Console.YELLOW}$destDir${scala.Console.RESET}"
    )
    SHARED_DIRS.foreach { d =>
      log.info(
        s"Shared dir  : ${scala.Console.YELLOW}${destDir.resolve(d)}${scala.Console.RESET}  →  ${scala.Console.YELLOW}${appDir.resolve(d)}${scala.Console.RESET}"
      )
    }
    symlinks(linkRoot, stageDir.resolve("bin"), destDir.resolve("bin"), isWindows).foreach { case (link, d) =>
      log.info(
        s"To be linked: ${scala.Console.YELLOW}$link${scala.Console.RESET}  →  ${scala.Console.YELLOW}$d${scala.Console.RESET}"
      )
    }
  }

  private lazy val staleInstallationsTaskImpl: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    import sbt.complete.DefaultParsers._

    val deployArgOpt = (Space.? ~> StringBasic.?).parsed
    val ctx          = deployCtx.value
    import ctx.{pName, log}

    lazy val deployRootFromEnv = getEnv(DEPLOY_PATH_ENV_NAME, log)

    val deployRoot = Paths.get(deployArgOpt.getOrElse(deployRootFromEnv))
    val appDir     = deployRoot.resolve(pName)
    staleInstallationsDef(appDir, pName, _ => true, log)
  }
}
