package com.stackifydev.springai.model;

import java.util.List;

/**
 * Shape we ask the model to fill. Spring AI appends JSON instructions
 * and maps the reply onto this record.
 */
public record StudyTip(String title, List<String> steps, String whyItMatters) {
}
