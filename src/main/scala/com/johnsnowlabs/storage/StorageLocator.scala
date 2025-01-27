/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.johnsnowlabs.storage

import com.johnsnowlabs.util.{ConfigHelper, ConfigLoader}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession

case class StorageLocator(database: String, storageRef: String, sparkSession: SparkSession) {

  private lazy val fileSystem = FileSystem.get(sparkSession.sparkContext.hadoopConfiguration)

  private val clusterTmpLocation: String = {
    val tmpLocation = getTmpLocation
    if (tmpLocation.startsWith("s3:/")) {
      tmpLocation
    } else {
      val tmpLocationPath = new Path(tmpLocation)
      fileSystem.mkdirs(tmpLocationPath)
      fileSystem.deleteOnExit(tmpLocationPath)
      tmpLocation
    }
  }

  val clusterFileName: String = {
    StorageHelper.resolveStorageName(database, storageRef)
  }

  val clusterFilePath: Path = {
    if (!getTmpLocation.startsWith("s3:/")) {
      Path.mergePaths(new Path(fileSystem.getUri.toString + clusterTmpLocation), new Path("/" + clusterFileName))
    } else new Path(clusterTmpLocation + "/" + clusterFileName)
  }

  val destinationScheme: String = if (getTmpLocation.startsWith("s3:/")) "s3" else fileSystem.getScheme

  private def getTmpLocation: String = {
    ConfigLoader.getConfigStringValue(ConfigHelper.storageTmpDir)
  }

}

object StorageLocator {
  def getStorageSerializedPath(path: String, folder: String, withinStorage: Boolean): Path =
    Path.mergePaths(new Path(path), new Path((if (withinStorage) "/storage/" else "/") + folder))
}