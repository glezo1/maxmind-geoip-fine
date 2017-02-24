package data_structures;

import com.glezo.ipv4.Ipv4;
import com.glezo.ipv4.UnparseableIpv4Exception;

public class Ip_start_end_property implements Comparable<Ip_start_end_property>
{
	private Long	double_a;
	private Long	double_b;
	private String	string;
	private Ipv4	ip_start;
	private Ipv4	ip_end;
	
	
	public Ip_start_end_property(Long a, Long b, String c) throws UnparseableIpv4Exception
	{
		this.double_a=a;
		this.double_b=b;
		this.string=c;
		this.ip_start=null;
		this.ip_end=null;
		if(this.double_a!=null)	
		{	
			this.ip_start=new Ipv4(this.double_a);	
		}
		if(this.double_b!=null)	
		{	
			this.ip_end=new Ipv4(this.double_b);		
		}
	}
	public Long		get_double_a()	{	return this.double_a;	}
	public Long		get_double_b()	{	return this.double_b;	}
	public String	get_String()	{	return this.string;		}
	
	public int compareTo(Ip_start_end_property o) 
	{
		return this.double_b.compareTo(this.double_a);
	}
	public String toString()
	{
		return this.ip_start+" - "+this.ip_end+" "+this.string;
	}
}
