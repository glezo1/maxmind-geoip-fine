package data_structures;

public class IpRangeMatch 
{
	private Long ipIntStartA;
	private Long ipIntEndA;
	private Long ipIntStartB;
	private Long ipIntEndB;
	
	public IpRangeMatch(Long a,Long b,Long c,Long d)
	{
		this.ipIntStartA=a;
		this.ipIntEndA=b;
		this.ipIntStartB=c;
		this.ipIntEndB=d;
	}
	public Long	getIpIntStartA()	{	return this.ipIntStartA;	}
	public Long	getIpIntEndA()		{	return this.ipIntEndA;		}
	public Long	getIpIntStartB()	{	return this.ipIntStartB;	}
	public Long	getIpIntEndB()		{	return this.ipIntEndB;		}
}
