package saiml.nn.backprop

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification
import saiml.data.{FisherIris, Mnist}


class BackpropClassifierTest extends Specification with LazyLogging {
  "BackpropClassifier" should {
    "classify the flowers from the Fisher Iris dataset" >> {
      val (trainingSet, testSet) = FisherIris.trainingAndTestDataFloat()

      // Normalize the input values to speed up learning
      def normalize(x: Float): Float = (x - 25) / 25

      val classifier = new BackpropClassifier(trainingSet, 3, 5000, normalize = normalize)

      val accuracy = (for ((species, params) <- testSet) yield {
        classifier.predict(params) == species
      }).count { x: Boolean => x } / testSet.size.toDouble

      accuracy must be_>(0.8)  // 0.96 is typical
    }

    "classify the handwritten digits from the MNIST dataset" >> {
      val seed = Some(0L)
      val nHidden = 70
      val nRepeat = 1  // increase for better results
      val learnRate = 1.0f
      val expectedAccuracy = 0.9  // 0.95 with nRepeat=20

      val (trainingSet, testSet) = Mnist.trainingAndTestData()

      val start = System.currentTimeMillis

      // Careful normalization is essential with this simple network.
      def scale(x: Float): Float = x / 256.0f // source byte range to (0; 1)
      val mean = trainingSet.flatMap(_._2).take(10000).map(scale).sum / 10000.0f
      def normalize(x: Float): Float = scale(x) - mean // balance around 0 for stability

      logger.info("creating the classifier")
      val classifier = new BackpropClassifier(trainingSet, nHidden, nRepeat, learnRate, normalize, seed)

      logger.info("checking the accuracy")
      val accuracy = (for ((digit, params) <- testSet) yield {
        classifier.predict(params) == digit
      }).count { x: Boolean => x } / testSet.size.toDouble

      val time = (System.currentTimeMillis - start) / 1000.0
      logger.debug(s"MNIST backprop time: ${time}s")

      accuracy must be_>(expectedAccuracy)
    }
  }
}