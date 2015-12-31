package com.pirate.jacksparrow

import java.io.{File, PrintWriter}

import org.jsoup.Jsoup

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Success, Try, Failure}

/**
  * Created by pnagarjuna on 29/12/15.
  */
object Main {


  def writerLoaner(filePair: (File, File))(f: (PrintWriter, PrintWriter) => Unit): Unit = {

    val dataFile = filePair._1
    val errorFile = filePair._2
    val dataWriter = new PrintWriter(dataFile)
    val errorWriter = new PrintWriter(errorFile)
    try {
      f(dataWriter, errorWriter)
    } catch {
      case ex: Exception =>
        println(s"exception writing to files ${ex.getMessage}")
        ex.printStackTrace()
    }
    finally {
      dataWriter.flush()
      errorWriter.flush()
      dataWriter.close()
      errorWriter.close()
    }

  }

  implicit class TryUtils(info: Try[String]) {

    def str: String = info match {
      case Success(sValue) => sValue
      case Failure(th) => "NOT_FOUND"
    }

    def err: String = info match {
      case Success(sValue) => sValue
      case Failure(th) => th.getMessage
    }

  }

  def dataToCSV(link: String, olxAdInfo: OlxAdInfo): String = {
    s"""${link}    ${olxAdInfo.phone.str}    ${olxAdInfo.brand.str}    ${olxAdInfo.model.str}    ${olxAdInfo.year.str}    ${olxAdInfo.kmsDriven.str}    ${olxAdInfo.location.str}    ${olxAdInfo.cost.str}    ${olxAdInfo.sinceline.str}    ${olxAdInfo.heading.str}"""
  }

  def errorToCSV(link: String, olxAdInfo: OlxAdInfo): String = {
    s"""${link}    ${olxAdInfo.phone.err}    ${olxAdInfo.brand.err}    ${olxAdInfo.model.err}    ${olxAdInfo.year.err}    ${olxAdInfo.kmsDriven.err}    ${olxAdInfo.location.err}    ${olxAdInfo.cost.err}    ${olxAdInfo.sinceline.err}    ${olxAdInfo.heading.err}"""
  }

  def stealPage(page: Int)(dataProcessor: (String, OlxAdInfo) => Unit): Unit = {

    val olx = new Olx {}

    val f = PirateUtils.getPageHtml(olx.olx(page)).map { wsResponse =>

      println(s"fetching page ${page} links")

      val body = wsResponse.body.toString

      println(s"status: ${wsResponse.status}")

      val links = olx.getOlxLinks(Jsoup.parse(body)).distinct

      println(s"links count: ${links.length}, some of the links may be empty.")

      links.map { opLink =>

        opLink.map { link =>

          println("fetching ad page ... ")

          val adPage = PirateUtils.getPageHtml(link)

          adPage map { wsResponse =>
            println(s"status: ${wsResponse.status}")
            val body = wsResponse.body.toString
            val info = olx.getAdInfo(Jsoup.parse(body))
            println(link -> info)
            dataProcessor(link, info)
          }
          adPage.recover { case th => println(s"fetching ad page failed: ${th.getMessage}") }
          println("waiting for ad page ...")
          Await.result(adPage, 1 minutes)
        }

        opLink.getOrElse {
          println("link empty")
        }

      }

    }

    Await.result(f, Duration.Inf)
  }

  def main(args: Array[String]): Unit = {

    if (args.length != 2 || Try(args(0).toInt).isFailure || Try(args(1).toInt).isFailure || Try(args(0).toInt > args(1).toInt).get) {
      println( """Usage: sbt "runMain <first page> <final page>" """)
      sys.exit(0)
    }

    val first = args(0).toInt
    val last = args(1).toInt

    val home = sys.props("user.home")
    val dataFolder = new File(s"${home}/olx-data")
    if (!dataFolder.exists()) {
      dataFolder.mkdirs()
    }

    val dataFile = new File(dataFolder, s"olx-scrapping-data-page-${first}-${last}.csv")
    val errorFile = new File(dataFolder, s"olx-scrapping-errors-page-${first}-${last}.csv")

    val filePair = dataFile -> errorFile

    writerLoaner(filePair) { (a, e) => {
      (first to last).foreach { page =>
        println(s"current page: ${page}")
        stealPage(page) { (link, data) =>
          a.println(dataToCSV(link, data))
          a.flush()
          if (data.productIterator.count(_.asInstanceOf[Try[String]].isFailure) > 0) {
            e.println(errorToCSV(link, data))
            e.flush()
          }
        }
      }
    }
    }

  }

}
