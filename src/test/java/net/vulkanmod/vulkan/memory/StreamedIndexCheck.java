package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.util.VUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public final class StreamedIndexCheck {
    public static void main(String[] args) {
        TestMemory memory = new TestMemory();
        TestBuffer buffer = new TestBuffer(memory);
        ByteBuffer shorts = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
        shorts.putShort((short) 99).putShort((short) 2).putShort((short) 0).putShort((short) 1).flip().position(2);
        ByteBuffer ints = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder());
        ints.putInt(70000).putInt(0).putInt(1).flip();

        buffer.copyBuffer(shorts, IndexBuffer.IndexType.SHORT);
        require(buffer.getOffset() == 0 && buffer.getUsedBytes() == 6, "short offset/size");
        require(shorts.position() == 2, "source position changed");
        require(memory.read(buffer, 0).getShort() == 2, "short order changed");

        buffer.copyBuffer(ints, IndexBuffer.IndexType.INT);
        require(buffer.getOffset() == 8 && buffer.getUsedBytes() == 20, "mixed index alignment");
        require(buffer.indexType == IndexBuffer.IndexType.INT, "32-bit type lost");
        require(memory.read(buffer, 8).getInt() == 70000, "32-bit index truncated");
        buffer.copyBuffer(shorts, IndexBuffer.IndexType.SHORT);
        long oldId = buffer.getId();
        buffer.copyBuffer(ints, IndexBuffer.IndexType.INT);
        require(buffer.retiredId == oldId && buffer.getId() != oldId, "old allocation not retired");
        require(buffer.getOffset() == 0 && buffer.getUsedBytes() == 12, "growth did not reset stream");
        require(memory.allocations.get(oldId).get(0) == shorts.get(2), "previous draw data overwritten");
        require(memory.read(buffer, 0).getInt() == 70000, "growth lost uploaded indices");
        buffer.reset();
        buffer.copyBuffer(shorts, IndexBuffer.IndexType.SHORT);
        require(buffer.getOffset() == 0 && buffer.indexType == IndexBuffer.IndexType.SHORT, "frame reset/type switch");
        System.out.println("Streamed indices PASS: order, source position, 16/32-bit alignment, growth, retirement, frame reset");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class TestBuffer extends IndexBuffer {
        long retiredId;

        TestBuffer(MemoryType memory) {
            super(32, memory);
        }

        @Override
        public void freeBuffer() {
            retiredId = getId();
        }
    }

    private static final class TestMemory extends MemoryType {
        final Map<Long, ByteBuffer> allocations = new HashMap<>();
        long nextId;

        TestMemory() {
            super(Type.HOST_LOCAL, null, null);
        }

        @Override
        void createBuffer(Buffer buffer, int size) {
            buffer.setBufferSize(size);
            buffer.setId(++nextId);
            allocations.put(buffer.getId(), ByteBuffer.allocateDirect(size));
        }

        @Override
        void copyToBuffer(Buffer buffer, long size, ByteBuffer source) {
            VUtil.memcpy(source, allocations.get(buffer.getId()).duplicate(), (int) size, buffer.getUsedBytes());
        }

        ByteBuffer read(Buffer buffer, int offset) {
            return allocations.get(buffer.getId()).duplicate().order(ByteOrder.nativeOrder()).position(offset);
        }

        @Override
        void copyFromBuffer(Buffer buffer, long size, ByteBuffer target) {
            throw new UnsupportedOperationException();
        }

        @Override
        boolean mappable() {
            return false;
        }
    }
}
