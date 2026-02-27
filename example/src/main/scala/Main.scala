import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory
import scala.jdk.CollectionConverters._

object Main {
  private val logger = LoggerFactory.getLogger("Main")

  def main(args: Array[String]): Unit = {
    println("=== JVM Arguments ===")
    ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.foreach(println)

    println("=== Command-line Arguments ===")
    args.foreach(println)

    println("=== application.conf ===")
    val config = ConfigFactory.load()
    config.entrySet().asScala.toList.sortBy(_.getKey).foreach { entry =>
      println(s"${entry.getKey} = ${entry.getValue.render()}")
    }

    logger.info("Application started")
  }
}
