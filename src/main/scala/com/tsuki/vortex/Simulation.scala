package com.tsuki.vortex

import zio._
import org.apache.pekko.actor.typed.{ActorSystem, ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import com.tsuki.vortex.Engine._
import com.tsuki.vortex.Domain._

object Simulation extends ZIOAppDefault:

  // We use a Promise to extract the Ledger Ref from the Actor System so the API can use it
  def guardian(ledgerPromise: Promise[Nothing, ActorRef[LedgerCommand]]): Behavior[Nothing] = 
    Behaviors.setup[Nothing] { ctx =>
      val ledger = ctx.spawn(AirspaceLedger(), "Sector-001")
      
      // Complete the promise so ZIO can see the ledger
      Unsafe.unsafe { implicit unsafe =>
         runtime.unsafe.run(ledgerPromise.succeed(ledger)).getOrThrowFiberFailure()
      }

      ctx.spawn(DroneActor("Alpha", ledger, Vector3D(0, 0, 100), Vector3D(0.5, 0, 0)), "Alpha")
      ctx.spawn(DroneActor("Bravo", ledger, Vector3D(50, 0, 100), Vector3D(-0.5, 0, 0)), "Bravo")
      Behaviors.empty
    }

  override def run =
    for {
      _ <- Console.printLine(">>> VORTEX API GATEWAY: INITIALIZING <<<")
      
      ledgerPromise <- Promise.make[Nothing, ActorRef[LedgerCommand]]
      
      // Start Pekko (Forked)
      system <- ZIO.attempt(ActorSystem(guardian(ledgerPromise), "VortexSystem"))
      
      // Wait for Ledger to be ready
      ledger <- ledgerPromise.await
      
      _ <- Console.printLine(">>> HTTP SERVER LISTENING ON http://localhost:8080/drones <<<")
      
      // Run HTTP Server (Blocks here)
      _ <- Api.server(system, ledger)
      
      _ <- ZIO.attempt(system.terminate())
    } yield ()
