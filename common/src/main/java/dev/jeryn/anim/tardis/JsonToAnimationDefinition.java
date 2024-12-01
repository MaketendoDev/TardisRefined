package dev.jeryn.anim.tardis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;
import whocraft.tardis_refined.TardisRefined;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static net.minecraft.client.animation.AnimationChannel.Interpolations.CATMULLROM;
import static net.minecraft.client.animation.AnimationChannel.Interpolations.LINEAR;
import static net.minecraft.client.animation.AnimationChannel.Targets.POSITION;
import static net.minecraft.client.animation.AnimationChannel.Targets.ROTATION;

/**
 * <h1>JsonToAnimationDefinition</h1>
 *
 * <p>This file magically allows me to load JSON based animations to a AnimationDefinition</p>
 * <p>It was a nightmare to get correct</p>
 * <p>All Rights Reserved</p>
 *
 * <h2>Credits</h2>
 * <ul>
 *   <li>Developed by: Jeryn</li>
 * </ul>
 *
 * @version 1.0
 * @since 2024-12-01
 */
public class JsonToAnimationDefinition {


    public static final AnimationChannel.Interpolation SNAP_TO = (destination, progress, keyFrames, startIndex, endIndex, scaleFactor) -> {
        Vector3f startVector = keyFrames[startIndex].target();
        Vector3f endVector = keyFrames[endIndex].target();
        return startVector.lerp(endVector, 0, destination).mul(scaleFactor);
    };


    public static AnimationDefinition loadAnimation(ResourceManager resourceManager, ResourceLocation resourceLocation) {

        JsonObject animationJson = loadJsonFromResource(resourceManager, resourceLocation);
        JsonArray animations = animationJson.getAsJsonArray("animations");

        if(animationJson == null || animations.isEmpty() || animations.isJsonNull()){
            TardisRefined.LOGGER.info("The specified animation '{}' does not exist?", resourceLocation);
        }

        float animationLength = animationJson.get("length").getAsFloat();

        AnimationDefinition.Builder animationDefinition = AnimationDefinition.Builder.withLength(animationLength);

        boolean looping = animationJson.get("looping").getAsBoolean();

        if(looping){
            animationDefinition.looping();
        }

        for (JsonElement boneEntry : animations.getAsJsonArray()) {
            JsonObject boneData = boneEntry.getAsJsonObject();
            System.out.println(boneData);
            List<Keyframe> rotationKeyframes = new ArrayList<>();
            List<Keyframe> positionKeyframes = new ArrayList<>();

            String boneName = boneData.get("bone").getAsString();

            System.out.println(boneData.get("target").getAsString());


            if(boneData.get("target").getAsString().equals("rotation")){
                rotationKeyframes = parseKeyframes(boneData, ROTATION);
            }

            if(boneData.get("target").getAsString().equals("position")){
                positionKeyframes = parseKeyframes(boneData, POSITION);
            }



            AnimationChannel positionChannel = positionKeyframes.isEmpty() ? null : new AnimationChannel(POSITION, positionKeyframes.toArray(new Keyframe[0]));
            AnimationChannel rotationChannel = rotationKeyframes.isEmpty() ? null : new AnimationChannel(ROTATION, rotationKeyframes.toArray(new Keyframe[0]));

            if (positionChannel != null) {
                animationDefinition.addAnimation(boneName, positionChannel);
            }
            if (rotationChannel != null) {
                animationDefinition.addAnimation(boneName, rotationChannel);
            }


        }

        return animationDefinition.build();
    }

    private static List<Keyframe> parseKeyframes(JsonElement transformationData, AnimationChannel.Target targetType) {
        List<Keyframe> keyframes = new ArrayList<>();

        if(transformationData == null) return keyframes;

        JsonObject jsonObject = transformationData.getAsJsonObject();

        System.out.println(jsonObject);

        if (!jsonObject.has("keyframes") || !jsonObject.get("keyframes").isJsonArray()) {
            return keyframes;
        }

        JsonArray keyframesArray = jsonObject.getAsJsonArray("keyframes");
        for (JsonElement keyframeElement : keyframesArray) {
            if (!keyframeElement.isJsonObject()) continue;

            JsonObject keyframeObject = keyframeElement.getAsJsonObject();

            // Parse the individual keyframe details
            float timestamp = keyframeObject.has("timestamp") ? keyframeObject.get("timestamp").getAsFloat() : 0.0f;
            JsonArray targetArray = keyframeObject.has("target") ? keyframeObject.getAsJsonArray("target") : null;
            AnimationChannel.Interpolation interpolation = keyframeObject.has("interpolation") ? getInterpolation(keyframeObject.get("interpolation").getAsString()) : getInterpolation("linear");

            Vector3f vector3f = new Vector3f();

            // Validate and convert the target array to a 3D vector (e.g., float[])
            float[] target = new float[3];
            if (targetArray != null && targetArray.size() == 3) {
                for (int i = 0; i < 3; i++) {
                    target[i] = targetArray.get(i).getAsFloat();
                }
            } else {
                continue; // Skip this keyframe if target is invalid
            }

            vector3f.set(target);
            System.out.println(vector3f);
            // Create a new Keyframe object and add it to the list
            Keyframe keyframe = new Keyframe(timestamp, targetType == POSITION ? KeyframeAnimations.posVec(vector3f.x, vector3f.y, vector3f.z) : KeyframeAnimations.degreeVec(vector3f.x, vector3f.y, vector3f.z), interpolation);
            keyframes.add(keyframe);
        }

        // Log the total number of keyframes parsed
        TardisRefined.LOGGER.info("Total keyframes parsed for target " + targetType + ": " + keyframes.size());

        return keyframes;
    }


    private static AnimationChannel.Interpolation getInterpolation(String easingType) {
        if(easingType.equals("linear")){
            return LINEAR;
        }

        if(easingType.equals("catmullrom")){
            return CATMULLROM;
        }

        return SNAP_TO;
    }


    public static JsonObject loadJsonFromResource(ResourceManager resourceManager, ResourceLocation resourceLocation) {
        try {
            InputStream inputStream = resourceManager.getResource(resourceLocation).get().open();
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}