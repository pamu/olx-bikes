package com.pirate.jacksparrow

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ning.NingWSClient

/**
  * Created by pnagarjuna on 29/12/15.
  */
object HttpHelper {

  implicit lazy val pirateActorSystem = ActorSystem("PirateActorSystem")
  implicit val materializer = ActorMaterializer()
  val client = NingWSClient()

}
