package org.xhtmlrenderer.render;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;

import java.awt.Font;

/**
 * Encapuslates anything style related in a Box. A separate bean is used to
 * avoid cluttering up Box more than necessary.
 */
public class Style {
    private CalculatedStyle calculatedStyle;

    private float marginTopOverride;

    private boolean marginTopOverrideSet = false;

    private float marginBottomOverride;

    private boolean marginBottomOverrideSet = false;

    private float parentWidth;

    public Style(CalculatedStyle calculatedStyle, float parentWidth) {
        this.calculatedStyle = calculatedStyle;
        this.parentWidth = parentWidth;
    }

    public Font getFont(CssContext cssContext) {
        return cssContext.getFont(calculatedStyle.getFont(cssContext));
    }

    public boolean isClearLeft() {
        IdentValue clear = calculatedStyle.getIdent(CSSName.CLEAR);
        return clear == IdentValue.LEFT || clear == IdentValue.BOTH;
    }

    public boolean isClearRight() {
        IdentValue clear = calculatedStyle.getIdent(CSSName.CLEAR);
        return clear == IdentValue.RIGHT || clear == IdentValue.BOTH;
    }

    public boolean isCleared() {
        return !calculatedStyle.isIdent(CSSName.CLEAR, IdentValue.NONE);
    }

    public CalculatedStyle getCalculatedStyle() {
        return calculatedStyle;
    }

    public IdentValue getBackgroundRepeat() {
        return calculatedStyle.getIdent(CSSName.BACKGROUND_REPEAT);
    }

    public IdentValue getBackgroundAttachment() {
        return calculatedStyle.getIdent(CSSName.BACKGROUND_ATTACHMENT);
    }

    public boolean isAbsolute() {
        return calculatedStyle.isIdent(CSSName.POSITION, IdentValue.ABSOLUTE);
    }

    public boolean isFixed() {
        return calculatedStyle.isIdent(CSSName.POSITION, IdentValue.FIXED);
    }

    public boolean isFloated() {
        IdentValue floatVal = calculatedStyle.getIdent(CSSName.FLOAT);
        return floatVal == IdentValue.LEFT || floatVal == IdentValue.RIGHT;
    }
    
    public boolean isFloatedLeft() {
        return calculatedStyle.isIdent(CSSName.FLOAT, IdentValue.LEFT);
    }
    
    public boolean isFloatedRight() {
        return calculatedStyle.isIdent(CSSName.FLOAT, IdentValue.RIGHT);
    }

    public boolean isRelative() {
        return calculatedStyle.isIdent(CSSName.POSITION, IdentValue.RELATIVE);
    }

    public boolean isPostionedOrFloated() {
        return isAbsolute() || isFixed() || isFloated() || isRelative();
    }

    public float getMarginTopOverride() {
        return this.marginTopOverride;
    }

    public void setMarginTopOverride(float marginTopOverride) {
        this.marginTopOverride = marginTopOverride;
        this.marginTopOverrideSet = true;
    }

    public float getMarginBottomOverride() {
        return this.marginBottomOverride;
    }

    public void setMarginBottomOverride(float marginBottomOverride) {
        this.marginBottomOverride = marginBottomOverride;
        this.marginBottomOverrideSet = true;
    }

    public RectPropertySet getMarginWidth(CssContext cssContext) {
        RectPropertySet rect = 
            calculatedStyle.getMarginRect(parentWidth, parentWidth, cssContext).copyOf();

        // TODO: this is bad for cached rects...
        if (this.marginTopOverrideSet) {
            rect.setTop((int) this.marginTopOverride);
        }
        if (this.marginBottomOverrideSet) {
            rect.setBottom((int) this.marginBottomOverride);
        }
        return rect;
    }

    public boolean isAutoWidth() {
        return calculatedStyle.isIdent(CSSName.WIDTH, IdentValue.AUTO);
    }

    public boolean isAutoHeight() {
        // HACK: assume containing block height is auto, so percentages become
        // auto
        return calculatedStyle.isIdent(CSSName.HEIGHT, IdentValue.AUTO)
                || !calculatedStyle.hasAbsoluteUnit(CSSName.HEIGHT);
    }
    
    public boolean isAutoZIndex() {
        return calculatedStyle.isIdent(CSSName.Z_INDEX, IdentValue.AUTO);
    }

    public void setCalculatedStyle(CalculatedStyle calculatedStyle) {
        this.calculatedStyle = calculatedStyle;
    }
    
    public boolean establishesBFC() {
        IdentValue display = calculatedStyle.getIdent(CSSName.DISPLAY);
        IdentValue position = calculatedStyle.getIdent(CSSName.POSITION);
        
        return isFloated() || 
            position == IdentValue.ABSOLUTE || position == IdentValue.FIXED ||
            display == IdentValue.INLINE_BLOCK || display == IdentValue.TABLE_CELL ||
            ! calculatedStyle.isIdent(CSSName.OVERFLOW, IdentValue.VISIBLE);
    }
    
    public boolean requiresLayer() {
        IdentValue position = calculatedStyle.getIdent(CSSName.POSITION);
        
        return position == IdentValue.ABSOLUTE || position == IdentValue.RELATIVE ||
        position == IdentValue.FIXED;
    }
}
