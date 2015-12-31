package com.pirate.jacksparrow

import com.pirate.jacksparrow.commons.Site

/**
  * Created by pnagarjuna on 29/12/15.
  */
trait Quikr extends Site {

  def quikr(page: Int = 1) = s"http://bangalore.quikr.com/Bikes-Scooters/w264?page=$page"

}