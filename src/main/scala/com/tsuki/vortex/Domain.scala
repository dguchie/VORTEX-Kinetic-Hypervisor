package com.tsuki.vortex

import org.apache.pekko.actor.typed.ActorRef
import io.circe._, io.circe.generic.semiauto._

object Domain:
  // PHYSICS
  case class Vector3D(x: Double, y: Double, z: Double):
    def +(v: Vector3D): Vector3D = Vector3D(x + v.x, y + v.y, z + v.z)
    def distanceTo(v: Vector3D): Double =
      Math.sqrt(Math.pow(v.x - x, 2) + Math.pow(v.y - y, 2) + Math.pow(v.z - z, 2))

  // JSON ENCODERS
  implicit val vectorEncoder: Encoder[Vector3D] = deriveEncoder[Vector3D]

  // PROTOCOL
  sealed trait LedgerCommand
  case class ProcessTelemetry(id: String, position: Vector3D, replyTo: ActorRef[DroneCommand]) extends LedgerCommand
  // NEW: Command to read the state for the API
  case class GetSnapshot(replyTo: ActorRef[List[DroneSnapshot]]) extends LedgerCommand

  case class DroneSnapshot(id: String, position: Vector3D, status: String)
  implicit val snapshotEncoder: Encoder[DroneSnapshot] = deriveEncoder[DroneSnapshot]

  sealed trait DroneCommand
  case object Tick extends DroneCommand
  case class CollisionAlert(sourceId: String, distance: Double) extends DroneCommand
