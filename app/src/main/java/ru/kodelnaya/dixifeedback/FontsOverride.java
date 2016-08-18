package ru.kodelnaya.dixifeedback;

/**
 * Created by Noobburner on 11.08.2016.
 */
import java.lang.reflect.Field;
import android.content.Context;
import android.graphics.Typeface;

public final class FontsOverride {
    public static void replaceDefaultFont(Context context, String name, String nameFont){
        Typeface customFontTypeFace = Typeface.createFromAsset(context.getAssets(), nameFont);
        replaceFont(name, customFontTypeFace);

    }

    private static void replaceFont(String name, Typeface customFontTypeFace) {
        try {
            Field mtyfield = Typeface.class.getDeclaredField(name);
            mtyfield.setAccessible(true);
            mtyfield.set(null, customFontTypeFace);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}