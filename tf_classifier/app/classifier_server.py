from flask import Flask, request
from flasgger import Swagger

import json
import numpy as np

from sklearn.model_selection import train_test_split

import tensorflow as tf
import tensorflow_hub as hub
import tensorflow_text as text
from official.nlp import optimization

app = Flask("Topics Classifier!")
swagger = Swagger(app)

#Download H5 file from S3
#TODO!

#Class that defines a Predictor object to make all predictions
class Predictor:
    def __init__(self, model_path="topics_classifier_latest.h5", topics_path="topics.json"):
        """ 
        Constructor/initializer of the predictor. Loads the Keras model and label encoder for predictions
        """

        #Load model trained for 41 topics
        self.model = tf.keras.models.load_model(model_path, 
                                    custom_objects={"KerasLayer": hub.KerasLayer, 
                                                    "AdamWeightDecay": optimization.AdamWeightDecay})
        
        #Set dictioanry mapping labels
        with open(topics_path, "r") as f:
            self.classes = json.load(f)

    def predict(self, text):
        """
        Makes a prediction of the topic given the input text and returns the topic for it
        """
        predicted_class = np.argmax(self.model.predict([text]))
        return self.classes[str(predicted_class)]

#Initialize object to make all predictions
predictor = Predictor()

@app.route("/predict", methods=["POST"])
def predict():
    """ Endpoint to predict the topic for a given text
    ---
    parameters:
        - name: text
          in: formData
          type: string
          required: true
          description: Text to classify
    responses:
        200:
            description: "Predicted topic of text"
    """
    text = request.form["text"]
    #Call for prediction and return the answer
    return predictor.predict(text)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)