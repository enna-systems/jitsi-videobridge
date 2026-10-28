/*
 * Copyright @ 2026 enna systems GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.nlj.rtp.codec.vp9

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jitsi.nlj.MediaSourceDesc
import org.jitsi.nlj.PacketInfo
import org.jitsi.nlj.RtpEncodingDesc
import org.jitsi.nlj.rtp.codec.vpx.VpxRtpLayerDesc
import org.jitsi.nlj.util.bytes
import org.jitsi.utils.logging2.LoggerImpl

class Vp9ParserTest : FunSpec({
    test("flexible temporal layers include all lower temporal rates even when T2 arrives first") {
        val stream = TestStream()
        stream.parse(packet(keyframe = true))
        stream.parse(packet(tid = 2))
        stream.addBytes(0, 0, 1000)
        stream.addBytes(0, 1, 250)
        stream.addBytes(0, 2, 500)

        stream.layer(0, 1).getBitrate(100) shouldBe bitrateFor(1250)
        stream.layer(0, 2).getBitrate(100) shouldBe bitrateFor(1750)
    }

    test("a new scalability structure rebuilds temporal dependencies and retains measured rates") {
        val stream = TestStream()
        stream.parse(packet(keyframe = true))
        stream.parse(packet(tid = 2))
        stream.addBytes(0, 0, 1000)
        stream.addBytes(0, 1, 250)
        stream.addBytes(0, 2, 500)
        stream.parse(packet(keyframe = true))

        stream.layer(0, 2).getBitrate(100) shouldBe bitrateFor(1750)
        stream.source.getEncodingLayers(SSRC).count { it.sid == 0 } shouldBe 3
    }

    test("K-SVC includes temporal dependencies but excludes lower spatial layers") {
        val stream = TestStream()
        stream.parse(packet(keyframe = true))
        stream.parse(packet(sid = 2, tid = 2))
        for (sid in 0..2) {
            for (tid in 0..2) {
                stream.parse(packet(sid = sid, tid = tid))
                stream.addBytes(sid, tid, 1000)
            }
        }

        stream.layer(2, 2).getBitrate(100) shouldBe bitrateFor(3000)
    }

    test("full SVC includes spatial and temporal dependencies when the upper layer arrives first") {
        val stream = TestStream()
        stream.parse(packet(keyframe = true))
        stream.parse(packet(sid = 2, tid = 2, interLayerDependency = true))
        for (sid in 0..2) {
            for (tid in 0..2) {
                stream.parse(packet(sid = sid, tid = tid, interLayerDependency = sid > 0))
                stream.addBytes(sid, tid, 1000)
            }
        }

        stream.layer(2, 2).getBitrate(100) shouldBe bitrateFor(9000)
    }

    test("non-flexible picture-group temporal dependencies remain cumulative") {
        val stream = TestStream()
        stream.parse(packet(keyframe = true, flexible = false))
        stream.addBytes(0, 0, 1000)
        stream.addBytes(0, 1, 250)
        stream.addBytes(0, 2, 500)

        stream.layer(0, 2).getBitrate(100) shouldBe bitrateFor(1750)
    }
})

private const val SSRC = 123L

private class TestStream {
    val source = MediaSourceDesc(
        arrayOf(RtpEncodingDesc(SSRC, arrayOf(VpxRtpLayerDesc(0, 0, 0, 180, -1.0)))),
        "sender",
        "sender-v0"
    )
    private val parser = Vp9Parser(source, LoggerImpl("Vp9ParserTest"))

    fun parse(packet: Vp9Packet) = parser.parse(PacketInfo(packet))

    fun layer(sid: Int, tid: Int) = source.getEncodingLayers(SSRC).single { it.sid == sid && it.tid == tid }

    fun addBytes(sid: Int, tid: Int, bytes: Int) {
        layer(sid, tid).updateBitrate(bytes.bytes, 0)
    }
}

private fun bitrateFor(bytes: Int) = VpxRtpLayerDesc(0, 0, 0, 180, -1.0).apply {
    updateBitrate(bytes.bytes, 0)
}.getBitrate(100)

/** Minimal RTP/VP9 packets; the parser consumes descriptors, not decoded media. */
private fun packet(
    sid: Int = 0,
    tid: Int = 0,
    keyframe: Boolean = false,
    interLayerDependency: Boolean = false,
    flexible: Boolean = true
): Vp9Packet {
    val data = mutableListOf(0x80, 0x65, 0, 1, 0, 0, 0, 1, 0, 0, 0, SSRC.toInt())
    // I, L, B, E; P for delta frames, V for the scalability structure, F for flexible mode.
    data.add(0xac or (if (keyframe) 0x02 else 0x40) or (if (flexible) 0x10 else 0))
    data.addAll(listOf(0x80, 1, (tid shl 5) or (sid shl 1) or (if (interLayerDependency) 1 else 0)))
    if (!flexible) data.add(0) // TL0PICIDX
    if (flexible && !keyframe) data.add(2) // One P_DIFF=1, N=0.
    if (keyframe) {
        // Three spatial layers, resolutions present, picture group only in non-flexible mode.
        data.add(0x50 or (if (flexible) 0 else 8))
        data.addAll(listOf(0x01, 0x40, 0x00, 0xb4, 0x02, 0x80, 0x01, 0x68, 0x05, 0x00, 0x02, 0xd0))
        if (!flexible) data.addAll(listOf(4, 4, 4, 0x54, 1, 0x34, 2, 0x54, 1))
    }
    data.add(0) // Media payload is not interpreted by the layer parser.
    val buffer = data.map { it.toByte() }.toByteArray()
    return Vp9Packet(buffer, 0, buffer.size)
}
