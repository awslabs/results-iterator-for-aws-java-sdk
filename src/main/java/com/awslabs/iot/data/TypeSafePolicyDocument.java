package com.awslabs.iot.data;

import com.awslabs.general.helpers.implementations.GsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;

import java.nio.charset.StandardCharsets;

public class TypeSafePolicyDocument {
    public static final String EFFECT = "Effect";
    public static final String ACTION = "Action";
    public static final String RESOURCE = "Resource";
    public static final String STATEMENT = "Statement";
    public static final String VERSION = "Version";
    public final String Version;
    public final List<Statement> Statement;

    private TypeSafePolicyDocument(String version, List<Statement> statement) {
        this.Version = version;
        this.Statement = statement;
    }

    public static TypeSafePolicyDocument fromJson(String json) {
        HashMap<String, JsonElement> rawHashMap = GsonHelper.fromJson(HashMap.class, json.getBytes(StandardCharsets.UTF_8));

        String version = rawHashMap.get(VERSION)
                .map(JsonElement::getAsString)
                .getOrElse("2012-10-17");

        JsonElement rawStatement = rawHashMap.get(STATEMENT)
                .getOrElse(JsonObject::new);

        if (!rawStatement.isJsonArray()) {
            JsonArray temp = new JsonArray();
            temp.add(rawStatement);
            rawStatement = temp;
        }

        // Turn this into a list that is easier to work with
        List<JsonObject> jsonObjectList = List.ofAll((JsonArray) rawStatement)
                .map(JsonElement::getAsJsonObject);

        List<Statement> statementList = jsonObjectList
                // Does this have an Effect key?
                .filter(jsonObject -> jsonObject.has(EFFECT))
                // Get the Effect key in a tuple with the original object
                .map(jsonObject -> Tuple.of(jsonObject.get(EFFECT), jsonObject))
                // Is the Effect key a JSON primitive?
                .filter(tuple -> tuple._1.isJsonPrimitive())
                // Get the Effect key as a JSON primitive with the original object
                .map(tuple -> Tuple.of((JsonPrimitive) tuple._1, tuple._2))
                // Is the Effect key a string?
                .filter(tuple -> tuple._1.isString())
                // Get the Effect key as a string with the original object
                .map(tuple -> Tuple.of(tuple._1.getAsString(), tuple._2))
                // Does this have an Action key?
                .filter(tuple -> tuple._2.has(ACTION))
                // Get the Action key in a tuple with the original object
                .map(tuple -> Tuple.of(tuple._1, tuple._2.get(ACTION), tuple._2))
                // Is the Action key a JSON primitive or an array?
                .filter(tuple -> tuple._2.isJsonArray() || tuple._2.isJsonPrimitive())
                // Turn the Action value into a list of strings
                .map(tuple -> Tuple.of(tuple._1, toListOfStrings(tuple._2), tuple._3))
                // Does this have a Resource key?
                .filter(tuple -> tuple._3.has(RESOURCE))
                // Get the Resource key and finally ditch the original object
                .map(tuple -> Tuple.of(tuple._1, tuple._2, tuple._3.get(RESOURCE)))
                // Is the Resource key a JSON primitive or an array?
                .filter(tuple -> tuple._3.isJsonArray() || tuple._3.isJsonPrimitive())
                // Turn the Resource value into a list of strings
                .map(tuple -> Tuple.of(tuple._1, tuple._2, toListOfStrings(tuple._3)))
                .map(TypeSafePolicyDocument::toStatement)
                .toList();

        return new TypeSafePolicyDocument(version, statementList);
    }

    private static Statement toStatement(Tuple3<String, List<String>, List<String>> element) {
        return ImmutableStatement.builder()
                .effect(Effect.valueOf(element._1))
                .action(element._2)
                .resource(element._3)
                .build();
    }

    private static List<String> toListOfStrings(JsonElement jsonElement) {
        List<JsonPrimitive> tempList;

        if (jsonElement.isJsonPrimitive()) {
            tempList = List.of(jsonElement.getAsJsonPrimitive());
        } else {
            tempList = List.ofAll(jsonElement.getAsJsonArray())
                    .filter(JsonElement::isJsonPrimitive)
                    .map(JsonElement::getAsJsonPrimitive);
        }

        return tempList
                .filter(JsonPrimitive::isString)
                .map(JsonPrimitive::getAsString);
    }
}
