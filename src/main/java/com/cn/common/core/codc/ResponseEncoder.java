package com.cn.common.core.codc;

import org.apache.log4j.Logger;

import com.cn.common.core.model.Response;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * <pre>
 * 数据包格式
 * +——----——+——-----——+——----——+——----——+——----——+——----——+
 * |  包头	|  模块号      |  命令号    |  结果码    |  长度       |   数据     |  
 * +——----——+——-----——+——----——+——----——+——----——+——----——+
 * </pre>
 * 
 * @author -琴兽-
 *
 */
public class ResponseEncoder extends MessageToByteEncoder<Response> {
	public static Logger log = Logger.getLogger(ResponseEncoder.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, Response response, ByteBuf buffer) throws Exception {
		log.info("*****当前线程id: " + Thread.currentThread().getId());
		log.info("****返回请求:" + "module:" + response.getModule() + " cmd:" + response.getCmd() + " resultCode:"
				+ response.getStateCode());

		// 包头
		buffer.writeInt(ConstantValue.HEADER_FLAG);
		// module和cmd
		buffer.writeShort(response.getModule());
		buffer.writeShort(response.getCmd());
		// 结果码
		buffer.writeInt(response.getStateCode());
		// 长度
		int lenth = response.getData() == null ? 0 : response.getData().length;
		if (lenth <= 0) {
			buffer.writeInt(lenth);
		} else {
			buffer.writeInt(lenth);
			buffer.writeBytes(response.getData());
		}
	}
}
