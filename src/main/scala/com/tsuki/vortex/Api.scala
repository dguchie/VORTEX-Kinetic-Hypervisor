package com.tsuki.vortex

import zio._
import zio.http._
import io.circe.syntax._
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout
import com.tsuki.vortex.Domain._
import com.tsuki.vortex.Engine.AirspaceLedger
import scala.concurrent.duration._

object Api:
  def server(system: ActorSystem[Nothing], ledger: org.apache.pekko.actor.typed.ActorRef[LedgerCommand]): ZIO[Any, Throwable, Unit] =
    implicit val timeout: Timeout = Timeout(3.seconds)
    implicit val scheduler = system.scheduler

    val app = Http.collectZIO[Request] {
      case Method.GET -> Root / "drones" =>
        ZIO.fromFuture { ec => 
          ledger.ask[List[DroneSnapshot]](ref => GetSnapshot(ref))
        }.map { snapshots =>
          Response.json(snapshots.asJson.toString)
            .addHeader("Access-Control-Allow-Origin", "*")
        }.catchAll(e => 
          ZIO.succeed(Response.text(e.getMessage).withStatus(Status.InternalServerError))
        )
    }

    Server.serve(app).provide(Server.default)
