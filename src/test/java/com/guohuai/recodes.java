package com.guohuai;

import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.service.ComOrderBootService;
import com.guohuai.boot.pay.service.ReconciliationPassService;
import com.guohuai.boot.pay.vo.PaymentVo;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"deprecation","resource"})
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SettlementBoot.class)
@WebIntegrationTest("server.port:8883")
public class recodes {

	
	@Autowired
	ComOrderDao dao;
	@Autowired
	PaymentDao paydao;
	@Autowired
	ReconciliationPassService reconSevice;
	@Autowired
	ProtocolDao protocolDao;
	@Autowired
	ComOrderBootService bak;
	@Test
	public void contextLoads() {
//		PaymentForm req=new PaymentForm();
//		req.setReconciliationDate("2016-11-17");
//		try {
//			paymentService.findPaymentWithReconc(req);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.err.println(elementValConfig.merRSAKey);
//		System.err.println(elementValConfig.sign);
//		System.err.println(elementValConfig.merId);
		
	}
	
	@Test
	public void testWorkBook() {
	       try{
	           long curr_time=System.currentTimeMillis();
	           int rowaccess=10;//内存中缓存记录行数
	           /*keep 100 rowsin memory,exceeding rows will be flushed to disk*/
	           SXSSFWorkbook wb = new SXSSFWorkbook(rowaccess); 
	           int sheet_num=3;//生成3个SHEET

	           for(int i=0;i<sheet_num;i++){
	              Sheet sh = wb.createSheet();
	              //每个SHEET有60000ROW
	              for(int rownum = 0; rownum < 60000; rownum++) {
	                  Row row = sh.createRow(rownum);
	                  //每行有10个CELL
	                  for(int cellnum = 0; cellnum < 10; cellnum++) {
	                     Cell cell = row.createCell(cellnum);
	                     String address = new CellReference(cell).formatAsString();
	                     cell.setCellValue(address);
	                  }
	                  //每当行数达到设置的值就刷新数据到硬盘,以清理内存
	                  if(rownum%rowaccess==0){
	                     ((SXSSFSheet)sh).flushRows();
	                  }
	              }
	           }

	           /*写数据到文件中*/
	           FileOutputStream os = new FileOutputStream("d:/data/biggrid.xlsx");    
	           wb.write(os);
	           os.close();
	           /*计算耗时*/
	           System.out.println("耗时:"+(System.currentTimeMillis()-curr_time)/1000);
	       } catch(Exception e) {
	           e.printStackTrace();
	       }
	    }
	
//	@Test
//	public void testInsert(){
//		String userOid = "test1";
//		List<OrderVo> list = new ArrayList<OrderVo>();
//		for(int i=0;i<=9600;i++){
//			OrderVo vo = new OrderVo();
//			vo.setUserOid(userOid);
//			list.add(vo);
//			System.out.println("第"+i+"条");
//		}
//		dao.save(list);
//	}
	
	//模拟补单
	@Test
	public void test(){
		PaymentVo paymentVo = paydao.findByPayNo("8420022017032900000002");
//		bak.feed(paymentVo);
	}	
	
	//模拟对账文件入库
	@Test
	public void test2(){
		Map<String,String> returnMap = new HashMap<String,String>();
		String checkDate = "";
		String channelId = "10";
		String fileName = "C:/Users/hans/Desktop/100000178-baofoo-fi-2017-03-29.txt";
		returnMap = reconSevice.BFgetReconAndInstall(returnMap, checkDate, channelId, fileName);
//		System.out.println("100000178201703291225150000002432".length());
	}	
	
	
	

}
