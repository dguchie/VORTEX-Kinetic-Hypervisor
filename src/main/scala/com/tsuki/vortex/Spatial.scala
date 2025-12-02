package com.tsuki.vortex

import com.uber.h3core.H3Core
import com.tsuki.vortex.Domain.Vector3D

object Spatial:
  // Initialize the native H3 library
  private val h3 = H3Core.newInstance()
  
  // Resolution 11 ~25 meter edge length (Perfect for drones)
  private val RESOLUTION = 11
  
  // Reference Origin: San Francisco (37.7749, -122.4194)
  private val REF_LAT = 37.7749
  private val REF_LON = -122.4194

  /**
   * Converts Local Vector3D (meters) to a Hexagonal Address (H3 Index)
   * Approximation: 1 degree lat ~= 111km. 
   */
  def toIndex(v: Vector3D): Long =
    // Crude projection for MVP speed (Meters -> Degrees)
    val latOffset = v.y / 111111.0
    val lonOffset = v.x / (111111.0 * Math.cos(Math.toRadians(REF_LAT)))
    h3.latLngToCell(REF_LAT + latOffset, REF_LON + lonOffset, RESOLUTION)

  /**
   * Returns the central cell and all immediate neighbors (Disk of radius 1)
   */
  def getSearchSpace(index: Long): java.util.List[java.lang.Long] =
    h3.gridDisk(index, 1) // 1 = Immediate neighbors only
