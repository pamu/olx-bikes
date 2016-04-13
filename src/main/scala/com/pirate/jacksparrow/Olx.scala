package com.pirate.jacksparrow

import com.pirate.jacksparrow.commons.Site
import org.jsoup.nodes.Document
import scala.collection.JavaConversions._
import scala.util.{Success, Failure, Try}

/**
  * Created by pnagarjuna on 29/12/15.
  */

case class OlxAdInfo(
                      phone: Try[String],
                      brand: Try[String],
                      model: Try[String],
                      year: Try[String],
                      kmsDriven: Try[String],
                      cost: Try[String],
                      location: Try[String],
                      sinceline: Try[String],
                      time: Try[String],
                      heading: Try[String]
                    )

trait Olx extends Site {

  def olx(page: Int = 1) = s"http://olx.in/bangalore/bikes/?page=$page"

  def getOlxLinks(parsedLinksPage: Document): List[Option[String]] = {
    val parsedBody = parsedLinksPage
    val tables = parsedBody.getElementsByTag("table").toList
    val atags =
      for {
        table <- tables
        tbody <- table.getElementsByTag("tbody")
        atag <- tbody.getElementsByTag("a")
      } yield atag

    atags.map { atag =>
      val valid = atag.hasAttr("href")
      if (valid) {
        val href = atag.attr("href")
        if (href.trim.startsWith("http")) Some(href) else None
      } else None
    }.distinct

  }

  def getAdInfo(implicit parsedAdPage: Document): OlxAdInfo = {
    OlxAdInfo(
      getAdPhone,
      getAdBrand,
      getAdModel,
      getAdYear,
      getAdKMSDriven,
      getAdCost,
      getAdPostingLocation,
      getAdSinceline,
      getAdTime,
      getAdHeading
    )
  }

  def getAdPhone(implicit parsedAdPage: Document): Try[String] = {
    import scala.collection.JavaConversions._

    val phoneOut =
      for {
        elem <- parsedAdPage.getElementsByClass("contactitem").headOption
        phoneElem <- elem.getElementsByTag("strong").headOption
        phone <- Some(phoneElem.text())
      } yield phone

    phoneOut match {
      case Some(value) => Success(value)
      case None => Failure(OptionIsNoneException("Option is None"))
    }
  }

  case class OptionIsNoneException(str: String) extends Exception(str)

  def getStringFromBlock(implicit parsedAdPage: Document): Try[List[String]] = {
    import scala.collection.JavaConversions._
    Try {
      val itemOut =
        for {
          details <- parsedAdPage.getElementsByClass("details").toList
          item <- details.getElementsByClass("pding5_10")
        } yield item
      itemOut.map(_.text)
    }
  }

  private def parseStrings(filterRegex: String)(implicit parsedPage: Document) =
    getStringFromBlock.flatMap { stringList =>
      val opHead = stringList.filter(_.trim.startsWith(filterRegex)).headOption
      opHead.flatMap { branch =>
        val strs = branch.split(":\\s+")
        if (strs.length > 1) {
          Some(strs(1))
        } else None
      } match {
        case Some(value) => Success(value)
        case None => Failure(NotFound("Parsed but couldn't get Brand"))
      }
    }

  def getAdBrand(implicit parsedAdPage: Document): Try[String] =
    parseStrings("Brand:")

  case class NotFound(str: String) extends Exception(str)

  def getAdModel(implicit parsedAdPage: Document): Try[String] =
    parseStrings("Model:")


  def getAdYear(implicit parsedAdPage: Document): Try[String] =
    parseStrings("Year:")

  def getAdKMSDriven(implicit parsedPage: Document): Try[String] =
    parseStrings("KM's driven:").map { kms =>
      if (kms.contains("km")) {
        kms replaceAll("km", "") replaceAll(",", "") trim
      } else kms
    }

  def getAdHeading(implicit parsedAdPage: Document): Try[String] = {
    import scala.collection.JavaConversions._
    Try {
      val headingOut =
        for {
          offerContent <- parsedAdPage.getElementsByClass("offercontentinner")
          headingTag <- offerContent.getElementsByClass("brkword")
          heading <- headingTag.text.trim
        } yield heading
      headingOut.mkString
    }
  }

  def getAdCost(implicit parsedAdPage: Document): Try[String] = {
    import scala.collection.JavaConversions._
    Try {
      val priceLabel =
        for {
          priceLabel <- parsedAdPage.getElementsByClass("pricelabel")
          priceTag <- priceLabel.getElementsByTag("strong")
          price <- priceTag.text().trim
        } yield price
      priceLabel.mkString
    }
  }

  def getAdPostingLocation(implicit parsedAdPage: Document): Try[String] = {
    Try {
      val posting =
        for {
          mapElem <- parsedAdPage.getElementsByClass("show-map-link")
          cityTag <- mapElem.getElementsByTag("strong")
          city <- cityTag.text
        } yield city
      posting.mkString
    }
  }

  def getAdSinceline(implicit parsedAdPage: Document): Try[String] = {
    Try {
      val sincelineOut =
        for {
          elem <- parsedAdPage.getElementsByClass("userdetails")
          sinceElem <- parsedAdPage.getElementsByClass("sinceline")
          sinceline <- sinceElem.text()
        } yield sinceline
      sincelineOut.mkString
    }
  }

  def getAdTime(implicit parsedAdPage: Document): Try[String] = {
    import scala.collection.JavaConversions._
    Try {
      val timeOut =
        for {
          elem <- parsedAdPage.getElementsByClass("brlefte5")
        } yield elem
      timeOut.head.text
    }
  }

}
