package org.morgorithm.frames.repository;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.morgorithm.frames.entity.Facility;
import org.morgorithm.frames.entity.Member;
import org.morgorithm.frames.entity.Status;
import org.morgorithm.frames.projection.AccessSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;


@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StatusRepositoryTests {
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private FacilityRepository facilityRepository;

    @Test
    public void testGetFacilityInInfo(){
        List<Object[]> result=statusRepository.getFacilityInInfo();

        for(Object[] a:result){
            System.out.println(Arrays.toString(a));
        }
    }

    @Test
    public void accessSetTest() {
//        List<AccessSet> accessSets = statusRepository.getAllAccessSet();
//        List<Object[]> aa = statusRepository.getStatusOverlapped(accessSets.get(0));
//        System.out.println(aa.toString());
//        accessSets.forEach(accessSet -> {
//            System.out.println(accessSet.asString());
//        });
    }

    @Test
    @Rollback(false)
    public void insertEnterData() {
        Member member = memberRepository.findById(1L).get();
        Facility facility = facilityRepository.findById(1L).get();
        Status status = Status.builder()
                .facility(facility)
                .state(Status.ENTER)
                .member(member)
                .temperature(36.5)
                .build();
        statusRepository.save(status);
    }

    @Test
    @Rollback(false)
    public void insertLeaveData() {
        Member member = memberRepository.findById(1L).get();
        Facility facility = facilityRepository.findById(1L).get();
        Status status = Status.builder()
                .facility(facility)
                .state(Status.LEAVE)
                .member(member)
                .temperature(38.5)
                .build();
        statusRepository.save(status);
    }

    @Test
    @Rollback(false)
    public void insertReliableStatusData(){ // ??? ??? ????????? ???????????? ?????? (????????? ????????? ????????? ????????? ??????, ????????? ??? ????????? ???????????? ??? ?????????)
        List<Member> members = memberRepository.findAll();
        LinkedList<Long> bnos = new LinkedList<>(facilityRepository.getAllBnos());
        List<Status> statusList = new ArrayList<>();
        Random random = new Random();
        long timeBasis = System.currentTimeMillis();
        for (Member member : members) {
            long timeDelta = (long) (Math.random() * 600 * 1000); // ?????? ?????? 0 ~ 500 ???
            int willEnterFacilityCount = 1 + (int) (Math.random() * 5); // ????????? ?????? ???  1~5???
            for (int i=0; i<willEnterFacilityCount; i++) {
                Collections.shuffle(bnos);
                Facility facility = facilityRepository.findById(bnos.getFirst().longValue()).get(); // ?????? ??????
                double temperature = 36.5 + random.nextGaussian() * 2; // 32.5 ~ 40.5
                Status statusIn = Status.builder()
                        .facility(facility)
                        .state(Status.ENTER)
                        .member(member)
                        .temperature(temperature)
                        .build();

                statusIn.setRegDate(new Date(timeBasis + timeDelta).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                statusList.add(statusIn);

                long timeIn = (long) (60000 + Math.random() * 300000); // ?????? ?????? 60??? ~ 360???

                if (Math.random() < 0.05) { // 5% ????????? ???????????? ?????????
                    break;
                }
                Status statusOut = Status.builder()
                        .facility(facility)
                        .state(Status.LEAVE)
                        .member(member)
                        .temperature(temperature + random.nextGaussian()) // +- 1
                        .build();

                timeDelta += timeIn;
                statusOut.setRegDate(new Date(timeBasis + timeDelta).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                statusList.add(statusOut);

                timeDelta += 10000 + (long) (Math.random() * 100000); // ?????? ?????? 10 ~ 109 ???
            }
        }

        statusList.sort(Comparator.comparing(Status::getRegDate));
        statusList.forEach(status -> {
            statusRepository.save(status);
        });
    }

    /*
    ????????????????????? ????????? ???????????? ?????? ?????????. ????????? mno??? ???????????? ????????? ????????? ???????????? in ?????? out?????? ??????
    ????????????. out??? ???????????? in??? ?????? ??? ?????? ??????.
     */
    //********************?????? ??????*****
    //Status ???????????? ????????? ????????? ?????? ?????? BaseEntity ??????????????? @CreatedDate ?????? ???????????????!!
    // ????????? ??????????????? ????????? ???????????? ???????????? ???????????? ?????? ????????? ?????? ????????? ????????????.
    //dashboard??? ????????? ???????????? ?????????????????? ????????? ??? ?????? ????????? ?????? ?????? ?????? ???????????? polling?????? ??????
    @Test
    @Rollback(false)
    public void insertStatusData(){
        List<Status> totalData=new ArrayList<>();
        int cnt=(int)(memberRepository.count());
        Boolean arr[]=new Boolean[1000];
        int barr[]=new int[1000];
        Arrays.fill(arr,false);
        Arrays.fill(barr,0);

        //memeber ??? ??? ????????? 5?????? status??? ????????????
        IntStream.rangeClosed(1,cnt*2).forEach(i->{
            List<Status> data=new ArrayList<>();
            Long mno=Long.valueOf((int)(Math.random()*cnt)+1);
            Member member=Member.builder().mno(mno).build();

            Long bno=0L;

            if(arr[mno.intValue()]==false){
                bno=((long)(Math.random()*10)+1);
                barr[mno.intValue()]=bno.intValue();
                arr[mno.intValue()]=true;
            }else{
                bno=Long.valueOf(barr[mno.intValue()]);
                arr[mno.intValue()]=false;
            }
            Facility facility= Facility.builder().bno(bno).building("building"+bno).build();

            //?????? ????????? ??????
            double rangeMin=35;
            double rangeMax=37.3;
            Random r = new Random();

                double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();


            //LocalDatetime Random****************************
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

            // Save current LocalDateTime into a variable
            LocalDateTime localDateTime = LocalDateTime.now();

            // Format LocalDateTime into a String variable and print
            String formattedLocalDateTime = localDateTime.format(dateTimeFormatter);
            System.out.println("Current Date: " + formattedLocalDateTime);

            //Get random amount of days between -9~0
            Random random = new Random();
            int randomAmountOfDays = random.nextInt(10);

            System.out.println("Random amount of days: " + randomAmountOfDays);

            //?????? ?????? ???????????? 3?????? ??? ?????? ?????? ?????? min, max??? ????????? nextInt(max+min+1)-min
            int randomAmountOfHours=random.nextInt(7)-3;
            System.out.println("Random amount of hours: " + randomAmountOfHours);

            //?????? ?????? ???????????? 60??? ?????? min, max??? ????????? nextInt(max+min+1)-min
            int randomAmountOfMinute=random.nextInt(121)-60;
            System.out.println("Random amount of minutes: " + randomAmountOfMinute);


            // Add randomAmountOfDays to LocalDateTime variable we defined earlier and store it into a new variable
            LocalDateTime futureLocalDateTime = localDateTime.minusDays(randomAmountOfDays).plusHours(randomAmountOfHours).plusMinutes(randomAmountOfMinute);


            // Format new LocalDateTime variable into a String variable and print
            String formattedFutureLocalDateTime = String.format(futureLocalDateTime.format(dateTimeFormatter));
            System.out.println("Date " + randomAmountOfDays + " days in future: " + formattedFutureLocalDateTime);
            System.out.println();
            //******************************************************

            //?????? ????????? ??????
            //????????? ?????? ????????? 1????????? ???????????? ?????? Double???????????? ???????????? String to Double ??????
            Status status=Status.builder().member(member).facility(facility).state(arr[mno.intValue()]).temperature(Double.valueOf(String.format("%.1f",+randomValue))).build();
            status.setRegDate(futureLocalDateTime);

            data.add(status);
            totalData.addAll(data);

        });

        for(Status s:totalData){

            System.out.println("mno:"+s.getMember());
            System.out.println("building:"+s.getFacility());
            System.out.println("status:"+s.getState());
            System.out.println("temperature"+s.getTemperature());
            statusRepository.save(s);
        }
    }

    @Test
    @Rollback(false)
    void dummyData(){
        Member member=Member.builder().mno(5L).build();
        Facility facility= Facility.builder().bno(2L).building("building"+2L).build();
        Status status=Status.builder().member(member).facility(facility).state(true).build();
        statusRepository.save(status);
    }

    @Test
    void testGetMemberFacility(){
        List<Object> result=statusRepository.getMemberFacility(80L);
        for(Object a:result){
            System.out.println(a.toString());
            System.out.println("bno:"+((Facility) a).getBno());
        }

    }

    @Test
    void testGetRegtDate(){
        List<Object[]> result=statusRepository.getRegDateAndState(80L);
        for(Object a:result){
            System.out.println(a.toString());
            System.out.println("regDate:"+((LocalDateTime) a).toString());
        }
    }

    //dashboard??? ????????? ???????????? ??????????????????
    //
    @Test
    @Rollback(false)
    void testAddTestDataForRealTimeStatusUpdate() throws InterruptedException {
        int cnt=(int)(memberRepository.count());
        System.out.println("cnt:"+cnt);
        cnt--;
        Long bno=0L;

        for(int i=0;i<100;i++){
            double rangeMin=34.5;
            double rangeMax=37.6;
            Random r = new Random();
            Boolean stat;
            int s=(int)Math.round( Math.random() );
            if(s==1)
                stat=true;
            else
                stat=false;
            double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
            LocalDateTime now=LocalDateTime.now();
            Long mno=Long.valueOf((int)(Math.random()*cnt)+1);
            bno=((long)(Math.random()*10)+1);
            Member member=Member.builder().mno(mno).build();
            Facility facility= Facility.builder().bno(bno).building("building"+bno).build();
            Status status=Status.builder().member(member).facility(facility).state(stat).temperature(Double.valueOf(String.format("%.1f",+randomValue))).build();
            status.setRegDate(now);
            statusRepository.save(status);
            System.out.println(status.toString());
            Thread.sleep(3000);
            System.out.println("????????? ??????");
        }

    }
    //dashboard??? ????????? ???????????? ?????????????????? ????????? ??? ?????? ????????? ?????? ?????? ?????? ???????????? polling?????? ??????
    @Test
    @Rollback(false)
    void testEventNowBooleanBuilderInsertDummyData(){
        List<Status> totalData=new ArrayList<>();
        int cnt=(int)(memberRepository.count());
        Boolean arr[]=new Boolean[1000];
        int barr[]=new int[1000];
        Arrays.fill(arr,false);
        Arrays.fill(barr,0);

        IntStream.rangeClosed(1,cnt*4).forEach(i->{
            List<Status> data=new ArrayList<>();
            Long mno=Long.valueOf((int)(Math.random()*cnt)+1);
            Member member=Member.builder().mno(mno).build();

            Long bno=0L;

            if(arr[mno.intValue()]==false){
                bno=((long)(Math.random()*10)+1);
                barr[mno.intValue()]=bno.intValue();
                arr[mno.intValue()]=true;
            }else{
                bno=Long.valueOf(barr[mno.intValue()]);
                arr[mno.intValue()]=false;
            }
            Facility facility= Facility.builder().bno(bno).building("building"+bno).build();

            //?????? ????????? ??????
            double rangeMin=35;
            double rangeMax=37.3;
            Random r = new Random();

            double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();


            //LocalDatetime Random****************************
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

            // Save current LocalDateTime into a variable
            LocalDateTime localDateTime = LocalDateTime.now();

            // Format LocalDateTime into a String variable and print
            String formattedLocalDateTime = localDateTime.format(dateTimeFormatter);
            System.out.println("Current Date: " + formattedLocalDateTime);

            //Get random amount of days ?????? ??????
            Random random = new Random();
            int randomAmountOfDays = random.nextInt(1);
            randomAmountOfDays*=-1;
            System.out.println("Random amount of days: " + randomAmountOfDays);

            //?????? ?????? ???????????? 7?????? ??? ?????? ?????? ?????? min, max??? ????????? nextInt(max+min+1)-min
            int randomAmountOfHours=random.nextInt(7)-7;
            System.out.println("Random amount of hours: " + randomAmountOfHours);

            //?????? ?????? ???????????? 60??? ?????? min, max??? ????????? nextInt(max+min+1)-min
            int randomAmountOfMinute=random.nextInt(121)-60;
            System.out.println("Random amount of minutes: " + randomAmountOfMinute);


            // Add randomAmountOfDays to LocalDateTime variable we defined earlier and store it into a new variable
            LocalDateTime futureLocalDateTime = localDateTime.plusDays(randomAmountOfDays).plusHours(randomAmountOfHours).plusMinutes(randomAmountOfMinute);


            // Format new LocalDateTime variable into a String variable and print
            String formattedFutureLocalDateTime = String.format(futureLocalDateTime.format(dateTimeFormatter));
            System.out.println("Date " + randomAmountOfDays + " days in future: " + formattedFutureLocalDateTime);
            System.out.println();
            //******************************************************

            //?????? ????????? ??????
            //????????? ?????? ????????? 1????????? ???????????? ?????? Double???????????? ???????????? String to Double ??????
            Status status=Status.builder().member(member).facility(facility).state(arr[mno.intValue()]).temperature(Double.valueOf(String.format("%.1f",+randomValue))).build();
            status.setRegDate(futureLocalDateTime);

            data.add(status);
            totalData.addAll(data);

        });

        for(Status s:totalData){

            System.out.println("mno:"+s.getMember());
            System.out.println("building:"+s.getFacility());
            System.out.println("status:"+s.getState());
            System.out.println("temperature"+s.getTemperature());
            statusRepository.save(s);

        }
    }
    @Test
    void getFacilityInInfoOneDay(){
        //?????? ??????
        LocalDateTime startDatetime = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(0,0,0)); //?????? 00:00:00
        LocalDateTime endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59,59));
        System.out.println("######from: "+startDatetime);
        System.out.println("######to: "+endDatetime);
        List<Object[]> result=statusRepository.getFacilityInInfoOneDay(startDatetime,endDatetime);
        for(Object a[]:result){
            System.out.println(a.toString());
            System.out.println(Arrays.toString(a));
        }
    }

    //????????? ???????????? 2??? ?????? ?????? ????????? ??????
    @Test
    @Rollback(false)
    void makeDummyDataFromTweoWeeksBefore(){
        int cnt=(int)(memberRepository.count());
        Long bno=0L;

        for(int i=0;i<100;i++){
            double rangeMin=34.5;
            double rangeMax=37.6;
            Random r = new Random();
            Boolean stat;
            int s=(int)Math.round( Math.random() );
            if(s==1)
                stat=true;
            else
                stat=false;
            double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
            LocalDateTime now=LocalDateTime.now().minusDays(20L);
            Long mno=Long.valueOf((int)(Math.random()*cnt)+1);
            bno=((long)(Math.random()*10)+1);
            Member member=Member.builder().mno(mno).build();
            Facility facility= Facility.builder().bno(bno).building("building"+bno).build();
            Status status=Status.builder().member(member).facility(facility).state(stat).temperature(Double.valueOf(String.format("%.1f",+randomValue))).build();
            status.setRegDate(now);
            statusRepository.save(status);

        }
    }
    @Test
    void getLatestDateTest(){
        List<Object> result=statusRepository.getLatestDate();
        LocalDateTime localDateTime=(LocalDateTime)result.get(0);
        for(Object r:result){
            System.out.println("Date:"+r.toString());
            System.out.println("LocalDateTime:"+localDateTime);
        }
    }
    //LocalDateTime??? ???????????? 1 ??????????????? ????????? ?????????
    @Test
    void testLocalDateTime(){
        List<Object> result = statusRepository.getLatestDate();
        LocalDateTime latestDateTime;
        latestDateTime = (LocalDateTime) result.get(0);
        String temp=latestDateTime.toString();

        String modifyTime="";
        int minusMillisecond;
        //s??? ????????? ????????? character??? ????????? string ????????????
        modifyTime=temp.substring(0,temp.length()-1);

        //minusMillisecond s??? ????????? ???????????? ????????????character??? ???????????? int??? ??????
        minusMillisecond=Integer.parseInt(String.valueOf(temp.charAt(temp.length()-1)));

        //1 ?????????
        minusMillisecond--;

        modifyTime+=Integer.toString(minusMillisecond);

        latestDateTime=LocalDateTime.parse(modifyTime);
        System.out.println("test localdatetime version:"+latestDateTime.toString());

    }
    @Test
    void testGetMaxStatusNum(){
        Status result=statusRepository.findTopByOrderByStatusnumDesc();
        Long num=result.getStatusnum();
        System.out.println("result:"+result.toString());

    }

    @Test
    void testGetMemberDailyTemperatureStatus(){
        List<Object[]> result=statusRepository.getMemberDailyTemperatureStatus(1L);
        HashMap<String,Double> temperature=new HashMap<String, Double>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM-dd");


        for(Object[] a:result){
            System.out.println(Arrays.toString(a));
            System.out.println("temperature"+a[1]);
            System.out.println("regDate:"+a[0]);
            LocalDateTime temp=(LocalDateTime)a[0];
            System.out.println();
            temperature.put(temp.format(formatter),(double)a[1]);
        }
        temperature.forEach((key,value)
            ->System.out.println("key: "+key+", value: "+value));



    }
}
