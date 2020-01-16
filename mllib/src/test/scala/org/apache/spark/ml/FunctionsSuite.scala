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

package org.apache.spark.ml

import org.apache.spark.SparkException
import org.apache.spark.ml.functions.vector_to_array
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.util.MLTest
import org.apache.spark.mllib.linalg.{Vectors => OldVectors}
import org.apache.spark.sql.functions.col

class FunctionsSuite extends MLTest {

  import testImplicits._

  test("test vector_to_array") {
    val df = Seq(
      (Vectors.dense(1.0, 2.0, 3.0), OldVectors.dense(10.0, 20.0, 30.0)),
      (Vectors.sparse(3, Seq((0, 2.0), (2, 3.0))), OldVectors.sparse(3, Seq((0, 20.0), (2, 30.0))))
    ).toDF("vec", "oldVec")

      val result = df.select(vector_to_array('vec), vector_to_array('oldVec))
      .as[(Seq[Double], Seq[Double])]
      .collect().toSeq

    val expected = Seq(
      (Seq(1.0, 2.0, 3.0), Seq(10.0, 20.0, 30.0)),
      (Seq(2.0, 0.0, 3.0), Seq(20.0, 0.0, 30.0))
    )
    assert(result === expected)

    val df2 = Seq(
      (Vectors.dense(1.0, 2.0, 3.0),
       OldVectors.dense(10.0, 20.0, 30.0), 1),
      (null, null, 0)
    ).toDF("vec", "oldVec", "label")


    for ((colName, valType) <- Seq(
        ("vec", "null"), ("oldVec", "null"), ("label", "java.lang.Integer"))) {
      val thrown1 = intercept[SparkException] {
        df2.select(vector_to_array(col(colName))).count
      }
      assert(thrown1.getCause.getMessage.contains(
        "function vector_to_array requires a non-null input argument and input type must be " +
        "`org.apache.spark.ml.linalg.Vector` or `org.apache.spark.mllib.linalg.Vector`, " +
        s"but got ${valType}"))
    }
  }
}
