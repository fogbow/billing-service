package cloud.fogbow.fs.core.util.accounting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.ComputeItem;
import cloud.fogbow.fs.core.models.VolumeItem;

public class RecordUtilsTest {

    private static final long RECORD_ID_1 = 1L;
    private static final long RECORD_ID_2 = 2L;
    private static final long RECORD_ID_3 = 0;
    private static final String ORDER_ID_1 = "orderId1";
    private static final String ORDER_ID_2 = "orderId2";
    private static final String ORDER_ID_3 = "orderId3";
    private static final String RESOURCE_TYPE_1 = "compute";
    private static final String RESOURCE_TYPE_2 = "volume";
    private static final String RESOURCE_TYPE_3 = "compute";
    private static final String REQUESTER_1 = "requester1";
    private static final String REQUESTER_2 = "requester2";
    private static final String REQUESTER_3 = "requester3";
    private static final long DURATION_RECORD_1 = 1500000000L;
    private static final long DURATION_RECORD_2 = 2000000000L;
    private static final long DURATION_RECORD_3 = 2500000000L;
    private static final String START_DATE_1 = "2000-01-01T00:00:00.000+00:00";
    private static final String START_TIME_1 = "2000-01-01T00:00:00.000+00:00";
    private static final String END_DATE_1 = "2000-01-01T03:03:03.300+00:00";
    private static final String END_TIME_1 = "2000-01-01T03:03:03.300+00:00";
    private static final String START_TIME_2 = "2000-01-01T00:00:00.000+00:00";
    private static final String START_DATE_2 = "2000-01-01T00:00:00.000+00:00";
    private static final String END_DATE_2 = "2000-01-01T03:03:03.300+00:00";
    private static final String END_TIME_2 = "2000-01-01T03:03:03.300+00:00";
    private static final String START_TIME_3 = "2000-01-01T00:00:00.000+00:00";
    private static final String START_DATE_3 = "2000-01-01T00:00:00.000+00:00";
    private static final String END_DATE_3 = "2000-01-01T03:03:03.300+00:00";
    private static final String END_TIME_3 = "2000-01-01T03:03:03.300+00:00";
    private static final long SPEC_ID_1 = 1L;
    private static final long SPEC_ID_2 = 3L;
    private static final long SPEC_ID_3 = 3L;
    private static final int RAM_RECORD_1 = 4;
    private static final int VCPU_RECORD_1 = 2;
    private static final int SIZE_RECORD_2 = 100;
    private static final int VCPU_RECORD_3 = 4;
    private static final int RAM_RECORD_3 = 8;
    private static final String STATE_RECORD_1 = "FULFILLED";
    private static final String STATE_RECORD_2 = "OPEN";
    private static final String STATE_RECORD_3 = "OPEN";
    private Record record;
    private Long paymentStartTime;
    private Long paymentEndTime;
    private static final long RECORD_START_TIME = 100;
    private static final long RECORD_END_TIME = 200;
    private static final int COMPUTE_VCPU = 1;
    private static final int COMPUTE_RAM = 2;
    private static final int VOLUME_SIZE = 50;
    private Timestamp startTimeTimestamp;
    private Timestamp endTimeTimestamp;
    private RecordUtils recordUtils;
    
    // test case 1: record start = payment start and record end = payment end
    
    // In this case, the record time is the difference between the 
    // payment end and start times.
    @Test
    public void testGetTimeFromRecordCase1() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 100L;
        paymentEndTime = 200L;
        
        assertEquals(new Double(paymentEndTime - paymentStartTime), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }

    // test case 2: record start < payment start and record end = payment end
    
    // In this case, the record time is the difference between the 
    // payment end and start times.
    @Test
    public void testGetTimeFromRecordCase2() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 150L;
        paymentEndTime = 200L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 3: record start > payment start and record end = payment end 
    
    // In this case, the record time is the difference between the 
    // payment end time and the record start time.
    @Test
    public void testGetTimeFromRecordCase3() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 50L;
        paymentEndTime = 200L;
        
        assertEquals(new Double(100.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 4: record start = payment start and record end < payment end
    
    // In this case, the record time is the difference between the record end time
    // and the payment start time.
    @Test
    public void testGetTimeFromRecordCase4() {
        setUpNormalRecordTimes();

        paymentStartTime = 100L;
        paymentEndTime = 250L;
        
        assertEquals(new Double(100.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 5: record start < payment start and record end < payment end
    
    // In this case, the record time is the difference between the record end time and 
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase5() {
        setUpNormalRecordTimes();
        
        paymentStartTime = 150L;
        paymentEndTime = 250L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 6: record start > payment start and record end < payment end
    
    // In this case, the record time is the difference between the record end time
    // and the record start time.
    @Test
    public void testGetTimeFromRecordCase6() {
        setUpNormalRecordTimes();

        paymentStartTime = 50L;
        paymentEndTime = 250L;
        
        assertEquals(new Double(100.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 7 (NORMAL): record start = payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase7() {
        setUpNullRecordEndTime();
        
        paymentStartTime = 100L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }

    // test case 8 (NORMAL): record start < payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase8() {
        setUpNullRecordEndTime();
        
        paymentStartTime = 150L;
        paymentEndTime = 180L;
        
        assertEquals(new Double(30.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 9 (NORMAL): record start > payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service.
    
    // In this case, the record time is the difference between the payment end time and
    // the record start time.
    @Test
    public void testGetTimeFromRecordCase9() {
        setUpNullRecordEndTime();
        
        paymentStartTime = 50L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 10 (RARE): record start = payment start and record end > payment end

    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service. However, it is possible that
    // the record ends after the payment end time is set and before the getRecord request
    // reaches the Accounting Service. In this case, the record has ended, its endTime
    // field is not null and its end time is after the payment end time.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase10() {
        setUpNormalRecordTimes();

        paymentStartTime = 100L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 11 (RARE): record start < payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service. However, it is possible that
    // the record ends after the payment end time is set and before the getRecord request
    // reaches the Accounting Service. In this case, the record has ended, its endTime
    // field is not null and its end time is after the payment end time.
    
    // In this case, the record time is the difference between the payment end time and
    // the payment start time.
    @Test
    public void testGetTimeFromRecordCase11() {
        setUpNormalRecordTimes();

        paymentStartTime = 150L;
        paymentEndTime = 180L;
        
        assertEquals(new Double(30.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case 12 (RARE): record start > payment start and record end > payment end
    
    // Record end time is after the payment end time. Normally, in this case,
    // the record endTime field is null, since the record has not ended yet and, thus, 
    // its end time is unknown by the Accounting Service. However, it is possible that
    // the record ends after the payment end time is set and before the getRecord request
    // reaches the Accounting Service. In this case, the record has ended, its endTime
    // field is not null and its end time is after the payment end time.
    
    // In this case, the record time is the difference between the payment end time and
    // the record start time.
    @Test
    public void testGetTimeFromRecordCase12() {
        setUpNormalRecordTimes();

        paymentStartTime = 50L;
        paymentEndTime = 150L;
        
        assertEquals(new Double(50.0), 
                recordUtils.getTimeFromRecord(record, paymentStartTime, paymentEndTime));
    }
    
    // test case: When calling the getItemFromRecord method passing a ComputeRecord, it must
    // extract the compute spec and build a ComputeItem correctly.
    @Test
    public void testGetComputeItemFromRecord() throws InvalidParameterException {
        this.record = Mockito.mock(Record.class);
        this.recordUtils = new RecordUtils();
        
        ComputeSpec computeSpec = new ComputeSpec(COMPUTE_VCPU, COMPUTE_RAM);
        
        Mockito.when(this.record.getResourceType()).thenReturn(ComputeItem.ITEM_TYPE_NAME);
        Mockito.when(this.record.getSpec()).thenReturn(computeSpec);

        
        ComputeItem item = (ComputeItem) this.recordUtils.getItemFromRecord(this.record);
        
        
        assertEquals(COMPUTE_VCPU, item.getvCPU());
        assertEquals(COMPUTE_RAM, item.getRam());
    }
    
    // test case: When calling the getItemFromRecord method passing a VolumeRecord, it must
    // extract the volume spec and build a VolumeItem correctly.
    @Test
    public void testGetVolumeItemFromRecord() throws InvalidParameterException {
        this.record = Mockito.mock(Record.class);
        this.recordUtils = new RecordUtils();
        
        VolumeSpec volumeSpec = new VolumeSpec(VOLUME_SIZE);
        
        Mockito.when(this.record.getResourceType()).thenReturn(VolumeItem.ITEM_TYPE_NAME);
        Mockito.when(this.record.getSpec()).thenReturn(volumeSpec);

        
        VolumeItem item = (VolumeItem) this.recordUtils.getItemFromRecord(this.record);
        
        
        assertEquals(VOLUME_SIZE, item.getSize());
    }
    
    // test case: When calling the getItemFromRecord method passing a Record of unknown
    // type, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetItemFromRecordUnknownType() throws InvalidParameterException {
        this.record = Mockito.mock(Record.class);
        this.recordUtils = new RecordUtils();
        
        Mockito.when(this.record.getResourceType()).thenReturn("unknowntype");

        this.recordUtils.getItemFromRecord(this.record);
    }    
    
    // test case: When calling the getRecordsFromString method, it must parse the given 
    // String and return a List containing Record instances of the correct types containing 
    // the data passed as argument.
    @Test
    public void testGetRecordsFromString() throws InvalidParameterException {
        this.recordUtils = new RecordUtils();

        RecordStringBuilder builder = new RecordStringBuilder();
        
        builder.addRecordString(new ComputeRecordString(RECORD_ID_1, ORDER_ID_1, RESOURCE_TYPE_1, 
                SPEC_ID_1, VCPU_RECORD_1, RAM_RECORD_1, REQUESTER_1, START_TIME_1, START_DATE_1, END_DATE_1, 
                END_TIME_1, DURATION_RECORD_1, STATE_RECORD_1));
        builder.addRecordString(new VolumeRecordString(RECORD_ID_2, ORDER_ID_2, RESOURCE_TYPE_2, 
                SPEC_ID_2, SIZE_RECORD_2, REQUESTER_2, START_TIME_2, START_DATE_2, END_DATE_2, 
                END_TIME_2, DURATION_RECORD_2, STATE_RECORD_2));
        builder.addRecordString(new ComputeRecordString(RECORD_ID_3, ORDER_ID_3, RESOURCE_TYPE_3, 
                SPEC_ID_3, VCPU_RECORD_3, RAM_RECORD_3, REQUESTER_3, START_TIME_3, START_DATE_3, END_DATE_3, 
                END_TIME_3, DURATION_RECORD_3, STATE_RECORD_3));
        
        String recordsString = builder.build();
        
        List<Record> records = this.recordUtils.getRecordsFromString(recordsString);
        
        Record record1 = records.get(0);
        
        assertEquals(RECORD_ID_1, record1.getId().longValue());
        assertEquals(ORDER_ID_1, record1.getOrderId());
        assertEquals(RESOURCE_TYPE_1, record1.getResourceType());
        assertEquals(SPEC_ID_1, record1.getSpec().getId().longValue());
        assertEquals(VCPU_RECORD_1, ((ComputeSpec) record1.getSpec()).getvCpu());
        assertEquals(RAM_RECORD_1, ((ComputeSpec) record1.getSpec()).getRam());
        assertEquals(REQUESTER_1, record1.getRequester());
        assertEquals(DURATION_RECORD_1, record1.getDuration());
        assertEquals(OrderState.valueOf(STATE_RECORD_1), record1.getState());
        
        Record record2 = records.get(1);
        
        assertEquals(RECORD_ID_2, record2.getId().longValue());
        assertEquals(ORDER_ID_2, record2.getOrderId());
        assertEquals(RESOURCE_TYPE_2, record2.getResourceType());
        assertEquals(SPEC_ID_2, record2.getSpec().getId().longValue());
        assertEquals(SIZE_RECORD_2, ((VolumeSpec) record2.getSpec()).getSize());
        assertEquals(REQUESTER_2, record2.getRequester());
        assertEquals(DURATION_RECORD_2, record2.getDuration());
        assertEquals(OrderState.valueOf(STATE_RECORD_2), record2.getState());
        
        Record record3 = records.get(2);
        
        assertEquals(RECORD_ID_3, record3.getId().longValue());
        assertEquals(ORDER_ID_3, record3.getOrderId());
        assertEquals(RESOURCE_TYPE_3, record3.getResourceType());
        assertEquals(SPEC_ID_3, record3.getSpec().getId().longValue());
        assertEquals(VCPU_RECORD_3, ((ComputeSpec) record3.getSpec()).getvCpu());
        assertEquals(RAM_RECORD_3, ((ComputeSpec) record3.getSpec()).getRam());
        assertEquals(REQUESTER_3, record3.getRequester());
        assertEquals(DURATION_RECORD_3, record3.getDuration());
        assertEquals(OrderState.valueOf(STATE_RECORD_3), record3.getState());
    }
    
    // test case: When calling the getRecordsFromString method and the given String
    // contains no record data, it must return an empty list.
    @Test
    public void testGetRecordsFromStringNoRecords() throws InvalidParameterException {
        this.recordUtils = new RecordUtils();
        RecordStringBuilder builder = new RecordStringBuilder();
        String recordsString = builder.build();
        
        List<Record> records = this.recordUtils.getRecordsFromString(recordsString);
        
        assertTrue(records.isEmpty());
    }
    
    // test case: When calling the getRecordsFromString method and the given String
    // contains records data from types other than "compute" and "volume", it
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetRecordsFromStringInvalidRecordType() 
            throws InvalidParameterException {
        this.recordUtils = new RecordUtils();
        
        String recordString = String.format(""
                + "{"
                + "    \"id\": %d,"
                + "    \"orderId\": \"%s\","
                + "    \"resourceType\": \"%s\","
                + "    \"spec\": {"
                + "        \"id\": %d,"
                + "        \"cidr\": \"%s\","
                + "        \"allocationMode\": \"%s\""
                + "    },"
                + "    \"requester\": \"%s\","
                + "    \"startTime\": \"%s\","
                + "    \"startDate\": \"%s\","
                + "    \"endDate\": \"%s\","
                + "    \"endTime\": \"%s\","
                + "    \"duration\": %d,"
                + "    \"state\": \"%s\""
                + "}", RECORD_ID_1, ORDER_ID_1, "network", SPEC_ID_1, "10.10.10.10/30", 
                "dynamic", REQUESTER_1, START_TIME_1, START_DATE_1, END_DATE_1, END_TIME_1, 
                DURATION_RECORD_1, STATE_RECORD_1);
        
        String recordsString = String.format("[%s]", recordString);
        
        this.recordUtils.getRecordsFromString(recordsString);
    }
    
    // test case: When calling the getRecordsFromString method and the given String
    // contains compute records data with missing properties, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetRecordsFromStringInvalidComputeRecord() 
            throws InvalidParameterException {
        this.recordUtils = new RecordUtils();
        
        String recordString = String.format(""
                + "{"
                + "    \"id\": %d,"
                + "    \"orderId\": \"%s\","
                + "    \"resourceType\": \"%s\","
                + "    \"spec\": {"
                + "        \"id\": %d,"
                + "        \"vCpu\": \"%d\","
                + "        \"ram\": \"%d\""
                + "    },"
                + "    \"requester\": \"%s\","
                + "    \"startDate\": \"%s\","
                + "    \"endDate\": \"%s\","
                + "    \"endTime\": \"%s\","
                + "    \"duration\": %d,"
                + "    \"state\": \"%s\""
                + "}", RECORD_ID_1, ORDER_ID_1, RESOURCE_TYPE_1, SPEC_ID_1, VCPU_RECORD_1, 
                RAM_RECORD_1, REQUESTER_1, START_DATE_1, END_DATE_1, END_TIME_1, 
                DURATION_RECORD_1, STATE_RECORD_1);
        
        String recordsString = String.format("[%s]", recordString);
        
        this.recordUtils.getRecordsFromString(recordsString);
    }
    
    // test case: When calling the getRecordsFromString method and the given String
    // contains volume records data with missing properties, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetRecordsFromStringInvalidVolumeRecord() 
            throws InvalidParameterException {
        this.recordUtils = new RecordUtils();
        
        String recordString = String.format(""
                + "{"
                + "    \"id\": %d,"
                + "    \"orderId\": \"%s\","
                + "    \"resourceType\": \"%s\","
                + "    \"spec\": {"
                + "        \"id\": %d,"
                + "        \"size\": \"%d\""
                + "    },"
                + "    \"requester\": \"%s\","
                + "    \"startDate\": \"%s\","
                + "    \"endDate\": \"%s\","
                + "    \"endTime\": \"%s\","
                + "    \"duration\": %d,"
                + "    \"state\": \"%s\""
                + "}", RECORD_ID_2, ORDER_ID_2, RESOURCE_TYPE_2, 
                SPEC_ID_2, SIZE_RECORD_2, REQUESTER_2, START_DATE_2, END_DATE_2, 
                END_TIME_2, DURATION_RECORD_2, STATE_RECORD_2);
        
        String recordsString = String.format("[%s]", recordString);
        
        this.recordUtils.getRecordsFromString(recordsString);
    }
    
    private void setUpNormalRecordTimes() {
        this.startTimeTimestamp = new Timestamp(RECORD_START_TIME);
        this.endTimeTimestamp = new Timestamp(RECORD_END_TIME);
        
        this.record = Mockito.mock(Record.class);
        Mockito.when(this.record.getStartTime()).thenReturn(startTimeTimestamp);
        Mockito.when(this.record.getEndTime()).thenReturn(endTimeTimestamp);
        
        this.recordUtils = new RecordUtils();
    }
    
    private void setUpNullRecordEndTime() {
        this.startTimeTimestamp = new Timestamp(RECORD_START_TIME);
        this.endTimeTimestamp = new Timestamp(RECORD_END_TIME);
        
        this.record = Mockito.mock(Record.class);
        Mockito.when(this.record.getStartTime()).thenReturn(startTimeTimestamp);
        Mockito.when(this.record.getEndTime()).thenReturn(null);
        
        this.recordUtils = new RecordUtils();
    }
    
    private abstract class RecordString {
        protected long id;
        protected String orderId;
        protected String resourceType;
        protected long specId;
        protected String requester;
        protected String startTime;
        protected String startDate;
        protected String endDate;
        protected String endTime;
        protected long duration;
        protected String state;
        
        public RecordString(long id, String orderId, String resourceType, 
                long specId, String requester, String startTime, String startDate, 
                String endDate, String endTime, long duration, String state) {
            this.id = id;
            this.orderId = orderId;
            this.resourceType = resourceType;
            this.specId = specId;
            this.requester = requester;
            this.startTime = startTime;
            this.startDate = startDate;
            this.endDate = endDate;
            this.endTime = endTime;
            this.duration = duration;
            this.state = state;
        }
        
        abstract String getString();
    }
    
    private class ComputeRecordString extends RecordString {
        private int vCPU;
        private int ram;
        
        public ComputeRecordString(long id, String orderId, String resourceType, 
                long specId, int vCPU, int ram, String requester, String startTime, 
                String startDate, String endDate, String endTime, long duration, String state) {
            super(id, orderId, resourceType, specId, requester, startTime, startDate, 
                    endDate, endTime, duration, state);
            this.vCPU = vCPU;
            this.ram = ram;
        }

        public String getString() {
            String recordString = String.format(""
                    + "{"
                    + "    \"id\": %d,"
                    + "    \"orderId\": \"%s\","
                    + "    \"resourceType\": \"%s\","
                    + "    \"spec\": {"
                    + "        \"id\": %d,"
                    + "        \"vCpu\": \"%d\","
                    + "        \"ram\": \"%d\""
                    + "    },"
                    + "    \"requester\": \"%s\","
                    + "    \"startTime\": \"%s\","
                    + "    \"startDate\": \"%s\","
                    + "    \"endDate\": \"%s\","
                    + "    \"endTime\": \"%s\","
                    + "    \"duration\": %d,"
                    + "    \"state\": \"%s\""
                    + "}", id, orderId, resourceType, specId, vCPU, ram, requester, 
                    startTime, startDate, endDate, endTime, duration, state);
            
            return recordString;
        }
    }
    
    private class VolumeRecordString extends RecordString {
        private int size;

        public VolumeRecordString(long id, String orderId, String resourceType, 
                long specId, int size, String requester, String startTime, 
                String startDate, String endDate, String endTime, long duration, String state) {
            super(id, orderId, resourceType, specId, requester, startTime, startDate, 
                    endDate, endTime, duration, state);
            this.size = size;
        }
        
        public String getString() {
            String recordString = String.format(""
                    + "{"
                    + "    \"id\": %d,"
                    + "    \"orderId\": \"%s\","
                    + "    \"resourceType\": \"%s\","
                    + "    \"spec\": {"
                    + "        \"id\": %d,"
                    + "        \"size\": \"%d\""
                    + "    },"
                    + "    \"requester\": \"%s\","
                    + "    \"startTime\": \"%s\","
                    + "    \"startDate\": \"%s\","
                    + "    \"endDate\": \"%s\","
                    + "    \"endTime\": \"%s\","
                    + "    \"duration\": %d,"
                    + "    \"state\": \"%s\""
                    + "}", id, orderId, resourceType, specId, size, requester, 
                    startTime, startDate, endDate, endTime, duration, state);
            
            return recordString;
        }
    }
    
    private class RecordStringBuilder {
        private List<RecordString> recordStrings;
        
        public RecordStringBuilder() {
            recordStrings = new ArrayList<RecordString>();
        }
        
        public void addRecordString(RecordString recordString) {
            recordStrings.add(recordString);
        }
        
        public String build() {
            List<String> strings = new ArrayList<String>();
            
            for (RecordString string : recordStrings) {
                strings.add(string.getString());
            }
            
            return String.format("[%s]", String.join(",", strings));
        }
    }
}
