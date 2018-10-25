package com.makor.hotornot.classifier

const val GRAPH_FILE_PATH = "file:///android_asset/frozen_inference_graph.pb"
const val LABELS_FILE_PATH = "file:///android_asset/labels.txt"

const val GRAPH_INPUT_NAME = "Placeholder"
const val GRAPH_OUTPUT_NAME = "final_result"

const val IMAGE_SIZE = 300
const val COLOR_CHANNELS = 3

const val ASSETS_PATH = "file:///android_asset/"

const val MAX_RESULTS = 300

const val MIN_CONF = 0.01

/*const*/ val OUTPUT_NAMES = arrayOf<String>("detection_boxes", "detection_scores", "detection_classes", "num_detections")  // TODO make this a private enum