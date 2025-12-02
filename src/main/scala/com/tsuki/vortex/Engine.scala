package com.tsuki.vortex

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{Behaviors}
import com.tsuki.vortex.Domain._
import com.tsuki.vortex.Spatial._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object Engine:
  object AirspaceLedger:
    // Holds the state of the world
    case class State(grid: Map[Long, Map[String, (Vector3D, ActorRef[DroneCommand])]])

    def apply(): Behavior[LedgerCommand] = active(State(Map.empty))

    private def active(state: State): Behavior[LedgerCommand] = Behaviors.receive { (ctx, msg) =>
      msg match
        case GetSnapshot(replyTo) =>
          // Flatten the H3 grid into a simple list for the API
          val allDrones = state.grid.values.flatMap(_.map { case (id, (pos, _)) =>
            DroneSnapshot(id, pos, "ACTIVE")
          }).toList
          replyTo ! allDrones
          Behaviors.same

        case ProcessTelemetry(id, pos, ref) =>
          val h3Index = Spatial.toIndex(pos)
          
          // Spatial Index Logic
          val cleanedGrid = state.grid.map { case (k, v) => k -> (v - id) }
          val currentBucket = cleanedGrid.getOrElse(h3Index, Map.empty)
          val newBucket = currentBucket + (id -> (pos, ref))
          val newGrid = cleanedGrid + (h3Index -> newBucket)

          // Collision Logic
          val searchIndices = Spatial.getSearchSpace(h3Index).asScala
          searchIndices.foreach { idx =>
            newGrid.get(idx).foreach { bucket =>
              bucket.foreach { case (otherId, (otherPos, otherRef)) =>
                if (otherId != id && pos.distanceTo(otherPos) < 20.0) {
                  ref ! CollisionAlert(otherId, pos.distanceTo(otherPos))
                  otherRef ! CollisionAlert(id, pos.distanceTo(otherPos))
                }
              }
            }
          }
          active(State(newGrid))
    }

  object DroneActor:
    def apply(id: String, ledger: ActorRef[LedgerCommand], startPos: Vector3D, velocity: Vector3D): Behavior[DroneCommand] =
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 100.millis)
        flying(id, ledger, startPos, velocity)
      }
    private def flying(id: String, ledger: ActorRef[LedgerCommand], pos: Vector3D, vel: Vector3D): Behavior[DroneCommand] =
      Behaviors.receive { (ctx, msg) =>
        msg match
          case Tick =>
            val newPos = pos + vel
            ledger ! ProcessTelemetry(id, newPos, ctx.self)
            flying(id, ledger, newPos, vel)
          case CollisionAlert(threat, dist) =>
            flying(id, ledger, pos, Vector3D(-vel.x, -vel.y, -vel.z))
      }
