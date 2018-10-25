package com.makor.hotornot.classifier

import android.graphics.RectF

public data class Prediction(val position: RectF, val classLabel: String, val score: Float)