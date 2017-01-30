package com.firetonton.tardisrecord.dummy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

    /**
     * A dummy item representing a piece of content.
     */
    public static class DummyItem {
        public final String id;
        public final File file;

        public DummyItem(File file) {
            this.id = file.getAbsolutePath();
            this.file = file;
        }

        @Override
        public String toString() {
            return file.getPath();
        }
    }
}
