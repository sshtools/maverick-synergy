/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 *  This class provides a pool for either direct or non direct ByteBuffers.
 */
public class ByteBufferPool
{
    private ArrayList<ByteBuffer> pool = new ArrayList<ByteBuffer>();
    private int capacity = 4096;
    private int allocated = 0;
    private long totalDirectMemoryAllocated = 0;
    
    /**
     * Create a default pool of ByteBuffers with 4k capacity
     */
    public ByteBufferPool() {
    }

    /**
     * Create a pool of ByteBuffers.
     *
     * @param capacity int
     * @param direct boolean
     */
    public ByteBufferPool(int capacity, boolean direct) {
        this.capacity=capacity;
    }

    /**
     * Get the capacity of buffers in this pool.
     *
     * @return int
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Get the number of buffers currently allocated out.
     *
     * @return int
     */
    public int getAllocatedBuffers() {
        return allocated;
    }

    /**
     * Get the number of buffers that are ready to be allocated.
     *
     * @return int
     */
    public int getFreeBuffers() {
        return pool.size();
    }

    /**
     * Calculate the total memory in use by this pool.
     *
     * @return long
     */
    public long getTotalMemoryInUse() {
        return totalDirectMemoryAllocated - (pool.size() * capacity);
    }

    /**
     * Calculate the total memory allocated by this pool.
     *
     * @return long
     */
    public synchronized long getTotalMemoryAllocated() {
        return totalDirectMemoryAllocated;
    }

    /**
     * Get a free buffer from the pool.
     *
     * @return ByteBuffer
     */
    public synchronized ByteBuffer get() {
        if (pool.isEmpty()) {

            allocated++;
            ByteBuffer buf = ByteBuffer.allocate(capacity);
            totalDirectMemoryAllocated += capacity;
            return buf;
        }

        
        ByteBuffer buffer = (ByteBuffer)pool.remove(pool.size()-1);
        buffer.clear();
        return buffer;
    }

    /**
     * Add a buffer back to the pool.
     *
     * @param buffer ByteBuffer
     */
    public synchronized void add(ByteBuffer buffer)
    {
        if(buffer==null)
            return;
        if (buffer.capacity()==capacity) {
            buffer.clear();
            pool.add(buffer);
        }
    }



}
