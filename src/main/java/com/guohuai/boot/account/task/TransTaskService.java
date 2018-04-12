package com.guohuai.boot.account.task;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.account.api.request.TransDetailQueryRequest;
import com.guohuai.account.api.response.TransDetailListResponse;
import com.guohuai.account.api.response.TransDetailQueryResponse;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.boot.account.service.TransService;

/**** 实现方法类 *****/
@Service
public class TransTaskService {
	private final static Logger log = LoggerFactory.getLogger(TransTaskService.class);
	private static final String NEWLINE = System.getProperty("line.separator", "\n");
	private static final String WORK_DIR = System.getProperty("user.dir");
	private static final String FILE_SEPARATOR = System.getProperty("file.separator", "/");
	
	@Autowired
	private TransService transService;
	
	@Transactional
	public void buildTransFile() throws Exception {
		TransDetailQueryRequest req = new TransDetailQueryRequest();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		Date startDate = DateUtil.lastDate(DateUtil.beginTimeInMillis());
		Date endDate = DateUtil.lastDate(DateUtil.endTimeInMillis());
		
		String startTime =  DateUtil.format(startDate, "yyyy-MM-dd HH:mm:ss");
		String endTime = DateUtil.format(endDate, "yyyy-MM-dd HH:mm:ss");
		
		File directory = null;  
		req.setStartTime(startTime);
		req.setEndTime(endTime);
		
		FileWriter fw = null;
		String fileName = "从" + sdf.format(startDate) + "到" + sdf.format(endDate) + "的交易信息";
		String suffix = ".csv"; 
		String filePath = WORK_DIR + FILE_SEPARATOR + "report" + FILE_SEPARATOR;
		directory = new File(filePath);  
        directory.mkdirs();  
        if (!directory.exists()) {  
            return;  
        }
		try {
			fw = new FileWriter(filePath+fileName+suffix, false);
			String header = "流水号,账户号,用户ID,用户类型,请求流水号,收单OID,订单类型,来源系统类型,来源系统单据号,关联产品编码,关联产品名称,金额方向,"
					+ "订单金额,备注,定单描述,账户名称,交易时间,数据来源,交易后余额,删除标记,币种,入账账户,出账账户,财务入账标识" + NEWLINE;
			fw.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
			fw.write(header);
			
			TransDetailListResponse resp = transService.tansDetailQueryList(req);
			List<TransDetailQueryResponse> transList = resp.getRows();
			int count = transList.size();
			if(transList != null && count > 0){
				for(int j = 0; j < count; j++){
					log.info("共" + count + "条数据,正在导出第" + (j + 1) + "条数据, 当前导出进度:" + String.format("%.2f", (j + 1.0) / count * 100.0) + "%");
					StringBuffer str = new StringBuffer();
					
					TransDetailQueryResponse trans = transList.get(j);
					
					str.append(trans.getOid() + ",");
					str.append(trans.getAccountOid() + ",");
					str.append(trans.getUserOid() + ",");
					str.append(trans.getUserType() + ",");
					str.append(trans.getRequestNo() + ",");
					str.append(trans.getAccountOrderOid() + ",");
					str.append(trans.getOrderType() + ",");
					str.append(trans.getSystemSource() + ",");
					str.append(trans.getOrderNo() + ",");
					str.append(trans.getRelationProductNo() + ",");
					str.append(trans.getRelationProductName() + ",");
					str.append(trans.getDirection() + ",");
					str.append(trans.getOrderBalance() + ",");
					str.append(trans.getRamark() + ",");
					str.append(trans.getOrderDesc() + ",");
					str.append(trans.getAccountName() + ",");
					str.append(trans.getUpdateTime() + ",");
					str.append(trans.getSystemSource() + ",");
					str.append(trans.getBalance() + ",");
					str.append(trans.getIsDelete() + ",");
					str.append(trans.getCurrency() + ",");
					str.append(trans.getInputAccountNo() + ",");
					str.append(trans.getOutpuptAccountNo() + ",");
					str.append(trans.getFinanceMark());
					str.append("\r\n");

					fw.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
					fw.write(str.toString());
					fw.flush();
				}
			}
			
			File file = new File(filePath+fileName+suffix);
            File zipFile = new File(filePath+fileName+".zip");
            InputStream input = new FileInputStream(file);
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
            zipOut.putNextEntry(new ZipEntry(file.getName()));
            int temp = 0;
            while((temp = input.read()) != -1){
                zipOut.write(temp);
            }
            input.close();
            zipOut.close();

            
			log.info("文件[" + filePath + "]生成完毕!!!");
			log.info("end");
		} catch (Exception e) {
			log.error("系统繁忙,导出交易流水失败", e);
		} finally {
			if (fw != null)
				fw.close();
		}
		
		
	}

}
