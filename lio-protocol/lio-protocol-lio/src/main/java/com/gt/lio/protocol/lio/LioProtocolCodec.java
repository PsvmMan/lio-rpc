package com.gt.lio.protocol.lio;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.protocol.ProtocolCodec;
import com.gt.lio.protocol.header.ProtocolHeader;
import com.gt.lio.protocol.ProtocolMessage;

import java.nio.ByteBuffer;
import java.util.List;

import static com.gt.lio.protocol.lio.LioProtocolConstants.*;

@SPIService("lio")
public class LioProtocolCodec implements ProtocolCodec {
    @Override
    public int decode(byte[] data, List<Object> out) throws Exception{

        // 已处理字节数
        int processed = 0;

        // 转化为ByteBuffer
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // 循环解析Lio协议，如果可读字节数大于等于Lio协议头的长度
        while (buffer.remaining() >= HEADER_TOTAL_LEN) {

            // ========== 开始查找有效的魔数 ==========
            boolean magicFound = false;

            while (buffer.remaining() >= 2) {
                // 记录当前位置
                int mark = buffer.position();

                // 尝试读取两个字节作为魔数
                short magic = buffer.getShort();

                if (magic == MAGIC) {
                    // 找到有效魔数，将 position 回退到魔数开始的位置
                    buffer.position(mark);
                    magicFound = true;
                    break;
                } else {
                    // 没找到魔数，向前移动一个字节，继续查找
                    buffer.position(mark + 1);
                    processed++; // 已处理了一个无效字节
                }
            }

            if (!magicFound) {
                return processed; // 没有找到魔数，返回已处理字节数
            }

            // 读取Lio协议头其他字段
            short magic = buffer.getShort();
            byte controlByte = buffer.get();
            byte metaDataByte = buffer.get();
            long requestId = buffer.getLong();
            int bodyLength = buffer.getInt();

            // 检查body是否完整，数据不完整，返回已处理字节数（当前头位置）
            if (buffer.remaining() < bodyLength) {
                return processed;
            }

            // 读取body
            byte[] body = new byte[bodyLength];
            buffer.get(body);

            // 构建消息对象
            LioHeader header = new LioHeader(magic, controlByte, metaDataByte, requestId, bodyLength);
            out.add(new ProtocolMessage(header, body));

            // 更新已处理字节数
            processed = buffer.position();
        }
        return processed;
    }

    @Override
    public byte[] encode(ProtocolMessage message) throws Exception {
        ProtocolHeader header = message.getHeader();
        int length = message.getBody() == null ? 0 : message.getBody().length;
        ByteBuffer buf = ByteBuffer.allocate(HEADER_TOTAL_LEN + length);

        // 设置协议头信息
        buf.putShort(MAGIC);
        buf.put(LioHeader.buildControlByte(header.getMsgType(), header.isRespond(), header.getSerializationType(), header.isCompressed(), header.getCompressionType()));
        buf.put(LioHeader.buildMetaDataByte(header.getThreadPoolName()));
        buf.putLong(header.getRequestId());
        buf.putInt(length);

        // 心跳消息，没有body
        if(length > 0){
            buf.put(message.getBody());
        }
        return buf.array();
    }

}