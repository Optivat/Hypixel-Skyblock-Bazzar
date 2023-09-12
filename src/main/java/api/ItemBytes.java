package api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemBytes {
    public String type;
    public IBValue value;
    public ItemBytes(String type, IBValue value) {
        this.type = type;
        this.value = value;
    }
    public static class IBValue {
        public String type;
        public ArrayList<IBValueList> list;
        public IBValue(String type, ArrayList<IBValueList> list) {
            this.type = type;
            this.list = list;
        }
    }
    public static class IBValueList {
        public Object id;
        public Object Count;
        public IBValueListTag tag;
        public IBValueList(Object id, Object Count, IBValueListTag tag) {
            this.id = id;
            this.Count = Count;
            this.tag = tag;
        }
    }
    public static class IBValueListTag {
        public String type;
        public IBVLValue value;
        public IBValueListTag(String type, IBVLValue value) {
            this.type = type;
            this.value = value;
        }
    }
    public static class IBVLValue {
        public Object Unbreakable;
        public IBVLExtraAttributes ExtraAttributes;

        public IBVLValue(Object Unbreakable, IBVLExtraAttributes ExtraAttributes) {
            this.Unbreakable = Unbreakable;
            this.ExtraAttributes = ExtraAttributes;
        }
    }
    public static class IBVLExtraAttributes {
        public String type;

        public IBVLExtraAttributesValue value;
        public IBVLExtraAttributes(String type, IBVLExtraAttributesValue value) {
            this.type = type;
            this.value = value;
        }
    }
    public static class IBVLExtraAttributesValue {
        public IBVLExtraAttributesValueID id;
        public Object uuid;
        public Object timestamp;
        public IBVLExtraAttributesValue(IBVLExtraAttributesValueID id, Object uuid, Object timestamp) {
            this.id = id;
            this.uuid = uuid;
            this.timestamp = timestamp;
        }
    }
    public static class IBVLExtraAttributesValueID {
        public String type;
        public String value;
        public IBVLExtraAttributesValueID(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}

