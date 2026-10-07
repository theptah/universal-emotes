package com.ptah.event.type;
import com.ptah.event.EmoteEvent;
public record ResetSkinEvent(float time) implements EmoteEvent {
    public String type() { return "reset_skin"; }
}
