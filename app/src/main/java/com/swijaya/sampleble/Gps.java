package com.swijaya.sampleble;

/**
 * Created by Murphy on 16/9/26.
 */
public class Gps {
    //ͶӰ��׼�����ͣ�����54��׼��Ϊ54������80��׼��Ϊ80��WGS84��׼��
    private static final int WGS84 = 84;
    private static final int BJ54 = 54;
    private static final int XIAN80 = 80;

    public static boolean confirmFlag = false;

    private String carId;

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    private double longitude;
    private double latitude;
    private double x;//x_location after transfer �ٶ���λΪ��
    private double y;//y_location after transfer

    private double speed;//����ÿСʱ����Ҫת��Ϊmÿ��
    private double angle;
    private double gpsTime;

    public Gps(){}

    public Gps(double longitude, double latitude, double speed, double angle) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.speed = speed;
        this.angle = angle;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLatitude() {
        return latitude;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public double getSpeed() {
        return speed;
    }

    public void setAngle(double angle){
        this.angle = angle;
    }
    public double getAngle(){
        return angle;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    //transfer angle to radian
    public double angleToRadian(double angle){
        double radian = angle * Math.PI / 180;
        return radian;
    }
    //���Ƕȷ�Χ��ӳ�䵽0-360��
    public double stdAngle(double angle){
        double stdAngle = 90 - angle;
        if (stdAngle < 0){
            stdAngle = stdAngle + 360;
        }
        return stdAngle;
    }

    /*	#define BJ54 54//����54��׼��Ϊ
        #define WGS84 84//WGS84��׼��Ϊ
        #define XIAN80 80//����80��׼��Ϊ
    */
    //����������ת��Ϊƽ������
    public void geodeticToCartesian ()
    {
        double L,B;//��ؾ��ȣ�γ��
        double l,L0;//ͶӰ������������ľ��ͶӰ������������
        double X,Y;//ֱ������
        double f,e2 = 0,a = 0,b,e12 = 0;////�ο����������,��һƫ���ʣ�������,�̰��ᣬ�ڶ�ƫ����
        double t;//����tan B
        double m;//����lcosB
        double q2;//î��Ȧ�ķ��� ��ƽ��
        double M,N;//���磬î��Ȧ���뾶
        double S;//�����߻���
        double A0,B0,C0,D0,E0;
        double n;//ͶӰ����
        double a_1_e2;//a*(1-e*e)��ֵ
        int zonewide=3;//3�ȴ��Ƚ�׼��������6�ȴ�

        int base_type=84;    //ͶӰ��׼�����ͣ�����54��׼��Ϊ54������80��׼��Ϊ80��WGS84��׼��
        final double PI = Math.PI;
        //----------��ʼ����֪����-----------
        L = this.longitude;//����
        B = this.latitude;//γ��
        if(WGS84 == base_type)
        {
            a = 6378137;//��λ�ǡ��ס�
            b = 6356752.3142;
            f = 1/298.257223563;
            e2 = 0.0066943799013;//--��һƫ����
            e12 = 0.00673949674227;//--�ڶ�ƫ����
        }
        else if(BJ54 == base_type)
        {
            a = 6378245;
            b = 6356863.0187730473;
            f = 1/298.3;
            e2 = 0.006693421622966;//--��һƫ����
            e12 = 0.006738525414683;//--�ڶ�ƫ����
        }
        else if(XIAN80 == base_type)
        {
            a = 6378140;
            b = 6356755.2881575287;
            f = 1/298.257;
            e2 = 0.006694384999588;//--��һƫ����
            e12 = 0.006739501819473;//--�ڶ�ƫ����
        }
        //---�������㹫ʽ a �����ᣬb �̰���--------
        //f = (a-b)/a;//--���������
        //e2 = (a*a-b*b)/(a*a);//--��һƫ����
        //e12 = (a*a-b*b)/(b*b);//--�ڶ�ƫ����
        a_1_e2 = a*(1-e2);
        //-----�򻯼������
        if (zonewide==6)
        {
            n=(int)(L/6)+1;
            L0=n*zonewide-3;
        }
        else
        {
            n=(int)((L-1.5)/3)+1;
            L0=n*3;
        }

        //---ת��Ϊ����,sin,cos �����㶼���Ի���Ϊ������
        L0=L0*PI/180;
        L=L*PI/180;
        B=B*PI/180;
        l = L - L0;
        t = Math.tan(B);
        q2 = e12*(Math.cos(B)*Math.cos(B));
        N = a/Math.sqrt(1-e2*Math.sin(B)*Math.sin(B));////î��Ȧ�����ʰ뾶
        m = l*Math.cos(B);
        //---�����߻���
        //--�����㷨�Ƕ�ԭʼ��ʽ�Ķ���ʽ�����˺ϲ�������������׼
        double m0 = a_1_e2;
        double m2 = 3.0 * e2 * m0 / 2.0;
        double m4 = 5.0 * e2 * m2 / 4.0;
        double m6 = 7.0 * e2 * m4 / 6.0;
        double m8 = 9.0 * e2 * m6 / 8.0;
        double a0 = m0 + m2 / 2.0 + 3.0 * m4 / 8.0 + 5.0 * m6 / 16.0 + 35.0 * m8 / 128;
        double a2 = m2 / 2.0 + m4 / 2.0 + 15.0 * m6 / 32.0 + 7.0 * m8 / 16.0;
        double a4 = m4 / 8.0 + 3.0 * m6 / 16.0 + 7.0 * m8 / 32.0;
        double a6 = m6 / 32.0 + m8 / 16.0;
        double a8 = m8 / 128.0;
        A0 = a0;
        B0 = a2;
        C0 = a4;
        D0 = a6;
        E0 = a8;
        S = (A0 * B - B0* Math.sin(2.0*B)/2.0 + C0 * Math.sin(4.0*B)/4.0 - D0 *Math.sin(6.0*B)/6.0 + E0*Math.sin(8.0*B)/8.0);

        //----��˹ͶӰ��ʽ-------
        X = S + N * t *((0.5 + ((5.0 - t * t + 9 * q2 +4 * q2 * q2)/24 + (61.0 - 58.0 * t * t + Math.pow(t,4)) * m * m /720.0) * m * m) * m * m);//pow(t,4)Ϊt��4�η�
        Y = N * ((1 + ((1 - t * t +q2)/6.0 +(5.0 - 18.0 * t * t + Math.pow(t,4) + 14 * q2 - 58 * q2 * t * t ) * m * m / 120.0) * m * m ) * m);
        //----�����й�����������������500���ﵱ����ʼ��
        Y = 1000000 * n + 500000 + Y;
        this.x = Y;//��Ϊ���Գ��ΪY��ģ��������ϱ�ΪY�����෴�ģ�����Y����x;
        this.y = X;/*δ��������*/
        //TRACE("x=%d , y=%d",pcc->x,pcc->y);
    }

    public double getGpsTime() {
        return gpsTime;
    }

    public void setGpsTime(double gpsTime) {
        this.gpsTime = gpsTime;
    }
}
