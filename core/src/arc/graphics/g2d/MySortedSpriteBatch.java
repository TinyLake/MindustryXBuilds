package arc.graphics.g2d;

import arc.graphics.*;
import arc.math.*;
import mindustryX.features.*;

import java.util.*;

public class MySortedSpriteBatch extends SortedSpriteBatch2{
    private static final boolean DEBUG = false;
    private static final int PRIME1 = 0xbe1f14b1;
    private static final int PRIME2 = 0xb4b82e39;
    int[] extraZ = new int[InitialSize];
    //增加小的delta，来保持原来的前后顺序
    int orderZ = 0;
    int hashZ = 0;//打乱hash值，来检查渲染异常

    @Override
    protected void z(float z){
        if(this.z == z) return;
        orderZ = 0;
        super.z(z);
    }

    @Override
    protected void flushRequests(){
        if(!flushing){
            DebugUtil.lastDrawRequests += numRequests;
            super.flushRequests();
        }
    }

    @Override
    protected void expandRequests(){
        super.expandRequests();
        extraZ = Arrays.copyOf(extraZ, requestZ.length);
    }

    private static final float[] tmpVertices = new float[24];

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        if(RenderExt.renderMerge){
            if(!Mathf.zero(rotation)){
                //bottom left and top right corner points relative to origin
                float worldOriginX = x + originX;
                float worldOriginY = y + originY;
                float fx = -originX;
                float fy = -originY;
                float fx2 = width - originX;
                float fy2 = height - originY;

                // rotate
                float cos = Mathf.cosDeg(rotation);
                float sin = Mathf.sinDeg(rotation);

                float x1 = cos * fx - sin * fy + worldOriginX;
                float y1 = sin * fx + cos * fy + worldOriginY;
                float x2 = cos * fx - sin * fy2 + worldOriginX;
                float y2 = sin * fx + cos * fy2 + worldOriginY;
                float x3 = cos * fx2 - sin * fy2 + worldOriginX;
                float y3 = sin * fx2 + cos * fy2 + worldOriginY;
                float x4 = x1 + (x3 - x2);
                float y4 = y3 - (y2 - y1);

                float u = region.u;
                float v = region.v2;
                float u2 = region.u2;
                float v2 = region.v;

                float color = this.colorPacked;
                float mixColor = this.mixColorPacked;

                tmpVertices[0] = x1;
                tmpVertices[1] = y1;
                tmpVertices[2] = color;
                tmpVertices[3] = u;
                tmpVertices[4] = v;
                tmpVertices[5] = mixColor;

                tmpVertices[6] = x2;
                tmpVertices[7] = y2;
                tmpVertices[8] = color;
                tmpVertices[9] = u;
                tmpVertices[10] = v2;
                tmpVertices[11] = mixColor;

                tmpVertices[12] = x3;
                tmpVertices[13] = y3;
                tmpVertices[14] = color;
                tmpVertices[15] = u2;
                tmpVertices[16] = v2;
                tmpVertices[17] = mixColor;

                tmpVertices[18] = x4;
                tmpVertices[19] = y4;
                tmpVertices[20] = color;
                tmpVertices[21] = u2;
                tmpVertices[22] = v;
                tmpVertices[23] = mixColor;
            }else{
                float fx2 = x + width;
                float fy2 = y + height;
                float u = region.u;
                float v = region.v2;
                float u2 = region.u2;
                float v2 = region.v;

                float color = this.colorPacked;
                float mixColor = this.mixColorPacked;

                tmpVertices[0] = x;
                tmpVertices[1] = y;
                tmpVertices[2] = color;
                tmpVertices[3] = u;
                tmpVertices[4] = v;
                tmpVertices[5] = mixColor;

                tmpVertices[6] = x;
                tmpVertices[7] = fy2;
                tmpVertices[8] = color;
                tmpVertices[9] = u;
                tmpVertices[10] = v2;
                tmpVertices[11] = mixColor;

                tmpVertices[12] = fx2;
                tmpVertices[13] = fy2;
                tmpVertices[14] = color;
                tmpVertices[15] = u2;
                tmpVertices[16] = v2;
                tmpVertices[17] = mixColor;

                tmpVertices[18] = fx2;
                tmpVertices[19] = y;
                tmpVertices[20] = color;
                tmpVertices[21] = u2;
                tmpVertices[22] = v;
                tmpVertices[23] = mixColor;
            }
            draw(region.texture, tmpVertices, 0, 24);
            return;
        }
        super.draw(region, x, y, originX, originY, width, height, rotation);
        if(sort && !flushing && RenderExt.renderSort){
            int h = region.texture.hashCode();
            if(DEBUG){
                h = (h + hashZ) * PRIME1;
                h = h ^ (h >>> 16);
            }
            extraZ[numRequests - 1] = ((orderZ++) << 16) | (h & 0xffff);
        }
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        super.draw(texture, spriteVertices, offset, count);
        if(sort && !flushing && RenderExt.renderSort){
            int h = texture.hashCode();
            if(DEBUG){
                h = (h + hashZ) * PRIME1;
                h = h ^ (h >>> 16);
            }
            extraZ[numRequests - 1] = ((orderZ++) << 16) | (h & 0xffff);
        }
    }

    @Override
    protected void draw(Runnable request){
        super.draw(request);
        if(sort && !flushing && RenderExt.renderSort){
            int h = DEBUG ? hashZ : 0;
            extraZ[numRequests - 1] = ((orderZ++) << 16) | (h & 0xffff);
        }
    }

    @Override
    protected void sortRequests(){
        if(!RenderExt.renderSort){
            super.sortRequests();
            return;
        }
        hashZ = DEBUG ? Float.floatToIntBits((float)Math.random()) : 0;

        int numRequests = this.numRequests;
        int[] arr = this.requestZ, extraZ = this.extraZ;
        sortMap(arr, numRequests);
        sortMap(extraZ, numRequests);
        for(int i = 0; i < numRequests; i++){
            arr[i] = (arr[i] << 16) | extraZ[i];
        }
        countingSortMap(arr, numRequests);//arr is loc now;

        if(copy.length < requests.length) copy = new DrawRequest[requests.length];
        final DrawRequest[] items = requests, dest = copy;
        for(int i = 0; i < numRequests; i++){
            dest[arr[i]] = items[i];
        }
    }

    private static final IntIntMap vMap = new IntIntMap(10000, 0.25f);
    private static int[] order = new int[1000];
    private static int[] order2 = new int[1000];

    /**
     * 将输入arr重映射到有序的[0,unique)域
     * @param arr 待排序数组，输出会映射为id值
     * @return unique
     */
    private static int sortMap(int[] arr, int len){
        var map = MySortedSpriteBatch.vMap;
        int[] order = MySortedSpriteBatch.order;
        map.clear();
        int unique = 0;
        for(int i = 0; i < len; i++){
            int v = arr[i];
            int id = map.getOrPut(v, unique);
            arr[i] = id;//arr现在表示id
            if(id == unique){
                if(order.length <= unique){
                    order = Arrays.copyOf(order, unique << 1);
                }
                order[unique] = v;
                unique++;
            }
        }
        MySortedSpriteBatch.order = order;

        //对z值排序
        Arrays.sort(order, 0, unique);//order -> z

        //arr中储存order
        int[] order2 = MySortedSpriteBatch.order2;//id -> order
        if(order2.length < order.length){
            order2 = new int[order.length];
            MySortedSpriteBatch.order2 = order2;
        }
        for(int i = 0; i < unique; i++){
            order2[map.getOrPut(order[i], -1)] = i;
        }
        for(int i = 0; i < len; i++){
            arr[i] = order2[arr[i]];
        }
        return unique;
    }

    /**
     * 计数排序
     * @param arr 待排序数组，输出为新loc
     */
    private static void countingSortMap(int[] arr, int len){
        int[] order = MySortedSpriteBatch.order, counts = MySortedSpriteBatch.order2;
        var map = MySortedSpriteBatch.vMap;//z->id
        map.clear();
        int unique = 0;
        for(int i = 0; i < len; i++){
            int v = arr[i];
            int id = map.getOrPut(v, unique);
            arr[i] = id;//arr现在表示id
            if(id == unique){
                if(unique >= counts.length){
                    order = Arrays.copyOf(order, unique << 1);
                    counts = Arrays.copyOf(counts, unique << 1);
                    MySortedSpriteBatch.order = order;
                    MySortedSpriteBatch.order2 = counts;
                }
                order[unique] = v;
                counts[unique] = 1;
                unique++;
            }else counts[id]++;
        }

        //对z值排序
        Arrays.sort(order, 0, unique);//order -> z

        //将counts转换为locs(每个id起始位置)
        for(int i = 0, loc = 0; i < unique; i++){
            int id = map.getOrPut(order[i], -1);
            int c = counts[id];
            counts[id] = loc;
            loc += c;
        }
        //arr现在表示新目的地
        for(int i = 0; i < len; i++){
            arr[i] = counts[arr[i]]++;
        }
    }

    static public class IntIntMap{
        private int[] keys;
        private boolean hasZero;
        private int[] values;
        private int zeroValue;
        private int size; // 哈希表中的元素数量

        private int capacity, maxSize;
        private float loadFactor;
        private int mask, hashShift;

        public IntIntMap(int capacity, float loadFactor){
            setCapacity(capacity, loadFactor);
        }

        private int hash(int key){
            key *= PRIME2;
            return (key ^ key >>> hashShift);
        }

        public int getOrPut(int key, int defaultValue){
            if(key == 0){
                if(hasZero) return zeroValue;
                zeroValue = defaultValue;
                hasZero = true;
                return defaultValue;
            }
            int mask = this.mask;
            int index = hash(key) & mask;
            int[] keys = this.keys;
            while(keys[index] != 0){
                if(keys[index] == key){// 键找到
                    return values[index];
                }
                index = (index + 1) & mask;
            }
            //键不存在
            keys[index] = key;
            values[index] = defaultValue;
            size++;
            if(size > maxSize) setCapacity(capacity << 1, loadFactor);
            return defaultValue;
        }

        private void setCapacity(int capacity, float loadFactor){
            capacity = Mathf.nextPowerOfTwo(capacity);
            this.capacity = capacity;
            this.loadFactor = loadFactor;
            maxSize = (int)(capacity * loadFactor);
            int mask = this.mask = capacity - 1;
            hashShift = 31 - Integer.numberOfTrailingZeros(capacity);

            int[] oldKeys = keys, oldValues = values;
            int[] keys = this.keys = new int[capacity];
            int[] values = this.values = new int[capacity];
            if(oldKeys == null || oldValues == null) return;
            for(int i = 0; i < oldKeys.length; i++){
                if(oldKeys[i] == 0) continue;
                int index = hash(oldKeys[i]) & mask;
                while(keys[index] != 0){
                    index = (index + 1) & mask;
                }
                keys[index] = oldKeys[i];
                values[index] = oldValues[i]; // 插入或更新值
            }
        }

        private void clear(){
            Arrays.fill(keys, 0);
            Arrays.fill(values, 0);
            size = 0;
            hasZero = false;
        }
    }
}
