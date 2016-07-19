package org.wowtools.pmgowiki;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Builder;

public class PmgoProbufTest {

	public static void main(String[] args) throws Exception {
		//构造一条数据
		Builder builder = PmgoProbuf.RequestEnvelop.newBuilder();
		builder.setLongitude(120);
		builder.setLatitude(30);
		builder.setUnknown1(1);
		RequestEnvelop req = builder.build();
		byte[] buf = req.toByteArray();
		
		//把序列化后的数据写入本地磁盘
		ByteArrayInputStream stream = new ByteArrayInputStream(buf);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("E:/protobuf.txt"));//设置输出路径
		BufferedInputStream bis = new BufferedInputStream(stream);
		int b = -1;
		while ((b = bis.read()) != -1) {
		bos.write(b);
		}
		bis.close();
		bos.close();
		
		//读取数据
		BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream("E:/protobuf.txt"));
		byte b2 = -1;
		List<Byte> list = new LinkedList<Byte>();
		while ((b2 = (byte) bis2.read()) != -1) {
			list.add(b2);
		}
		bis2.close();
		int length = list.size();
		byte[] byt = new byte[length];
		for(int i = 0; i < length; i++){
			byt[i] = list.get(i);
		}
		RequestEnvelop newReq = PmgoProbuf.RequestEnvelop.parseFrom(byt);
		System.out.println(newReq.getLongitude()+" "+newReq.getLatitude());
	}

}
