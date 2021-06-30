import json
import numpy as np
import pandas as pd

from sklearn.model_selection import train_test_split

import tensorflow as tf
import tensorflow_hub as hub
import tensorflow_text as text
from official.nlp import optimization  # to create AdamW optimizer

import matplotlib.pyplot as plt

tf.get_logger().setLevel('ERROR')


import boto3
import botocore
from pathlib import Path

def main():

    #Download JSON file if not present
    if not Path("dataset/News_Category_Dataset_v2.json").exists():
        BUCKET_NAME = 'factored-sandbox'
        KEY = 'david-avila/News_Category_Dataset_v2.json'
        s3 = boto3.resource('s3')
    
        try:
            s3.Bucket(BUCKET_NAME).download_file(KEY, "dataset/News_Category_Dataset_v2.json")
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                print("The object does not exist.")
            else:
                raise
    
    
    #Load dataset from JSON file
    data_inputs = []
    data_labels = []
    with open("dataset/News_Category_Dataset_v2.json", "r") as f:
        for line in f:
            article = json.loads(line)
            data_inputs.append(article["headline"])
            data_labels.append(article["category"])
    
    all_labels = np.array(list(set(data_labels)))
    num_topics = all_labels.shape[0]
    data_inputs = np.array(data_inputs)
    

    from sklearn.preprocessing import OneHotEncoder, LabelEncoder
    
    #Create one-hot encoding for the labels
    #1. Encode each label (topic) into num type
    #Label_encoder object knows how to understand word labels
    label_encoder = LabelEncoder()
    #Encode labels
    numeric_labels = label_encoder.fit_transform(data_labels)
    
    #2. Get one-hot encoding of each label
    onehotencoder = OneHotEncoder()
    #Reshape the 1-D label array to 2-D, as fit_transform expects 2-D, and fit the array 
    numeric_labels = onehotencoder.fit_transform(numeric_labels.reshape(-1,1)).toarray()
    
    
    #Split dataset to get train, test and validation splits (80-10-10)
    x_train, x_test, y_train, y_test = train_test_split(data_inputs, numeric_labels, test_size=0.1, random_state=1)
    x_train, x_val, y_train, y_val = train_test_split(x_train, y_train, test_size=0.111, random_state=1)
    

    #Select BERT model to use and fine-tune
    tfhub_handle_encoder = "https://tfhub.dev/tensorflow/small_bert/bert_en_uncased_L-4_H-512_A-8/1"
    tfhub_handle_preprocess = "https://tfhub.dev/tensorflow/bert_en_uncased_preprocess/3"
    

    from tensorflow.keras.layers import Input, Dropout, Dense
    from tensorflow.keras import Model
    
    #Load pre-processing model and main BERT model as Keras Layers
    #TODO: Adjust the last layer's activation if needed to classify (softmax maybe needed)
    bert_preprocess_layer = hub.KerasLayer(tfhub_handle_preprocess, name="preprocessing")
    bert_encoder_layer = hub.KerasLayer(tfhub_handle_encoder, trainable=True, name="BERT_encoder")
    #Create classifier model to fine-tune BERT outputs
    def build_classifier_model():
        text_input = Input(shape=(), dtype=tf.string, name='text')
        encoder_inputs = bert_preprocess_layer(text_input)
        outputs = bert_encoder_layer(encoder_inputs)
        net = outputs['pooled_output']
        net = Dropout(0.1)(net)
        net = Dense(num_topics, activation="softmax", name='classifier')(net)
        return Model(text_input, net)
    
    
    from tensorflow.keras.losses import CategoricalCrossentropy
    
    #Define loss function
    loss = CategoricalCrossentropy()
    
    #Define epochs and optimizer as AdamW
    epochs = 50
    steps_per_epoch = len(x_train)
    num_train_steps = steps_per_epoch * epochs
    num_warmup_steps = int(0.1*num_train_steps)
    
    init_lr = 3e-5
    optimizer = optimization.create_optimizer(init_lr=init_lr,
                                              num_train_steps=num_train_steps,
                                              num_warmup_steps=num_warmup_steps,
                                              optimizer_type='adamw')

    
    #Build and compile model with given loss and metrics needed
    classifier_model = build_classifier_model()
    classifier_model.compile(optimizer=optimizer,
                             loss=loss,
                             metrics=["accuracy"])
    
    class myCallback(tf.keras.callbacks.Callback):
        def on_epoch_end(self, epoch, logs={}):
            print("epoch completed!")
            if(logs.get('accuracy')>0.95):
                print("\nReached 95% accuracy so cancelling training!")
                self.model.stop_training = True
    
    #Fit model
    callback = myCallback()
    history = classifier_model.fit(x=x_train, y=y_train,
                                   validation_data=(x_val, y_val), 
                                   epochs=epochs,
                                   callbacks=[callback])
    
        
    
    #Save and export model to load it within Java TF and label classes as JSON
    classifier_model.save("topics_classifier.h5")
    dict_labels = dict(zip(label_encoder.classes_, range(0,num_topics)))
    with open("topics.json", "w") as f:
        json.dump(dict_labels, f)





