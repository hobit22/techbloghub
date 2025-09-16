package com.techbloghub.output.gpt.template;

public class SchemeTemplate {
    public static String getTaggingScheme() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "tags": {
                            "type": "array",
                            "items": {
                                "type": "string"
                            }
                        },
                        "categories": {
                            "type": "array",
                            "items": {
                                "type": "string"
                            }
                        }
                    },
                    "required": ["tags", "categories"],
                    "additionalProperties": false
                }
        """;
    }
}