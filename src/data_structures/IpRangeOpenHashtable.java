package data_structures;

import java.util.ArrayList;
import java.util.Collections;

import com.glezo.ipv4.UnparseableIpv4Exception;

public class IpRangeOpenHashtable 
{
	class Wrapper
	{
		private ArrayList<Ip_start_end_property> array_list;
		public Wrapper()
		{
			this.array_list=new ArrayList<Ip_start_end_property>();
		}
		public ArrayList<Ip_start_end_property> get_array_list(){return this.array_list;}
		public String toString(){return this.array_list.toString();}
	}
	
	private Wrapper	array[];
	private Integer	bits_used_as_hash;
	//-----------------------------------------------------------------------------------------------------
	public IpRangeOpenHashtable(int bits_used_as_hash,ArrayList<Ip_start_end_property> array_list)
	{
		this.bits_used_as_hash=bits_used_as_hash;
		double array_length=Math.pow(2,bits_used_as_hash);
		this.array=new Wrapper[(int)array_length];
		for(int i=0;i<array_length;i++){this.array[i]=null;}
		
		for(int i=0;i<array_list.size();i++)
		{
			Ip_start_end_property current=array_list.get(i);
			long ipstart_hash=current.get_double_a() >>> (32-bits_used_as_hash);
			if(this.array[(int)ipstart_hash]==null)
			{
				Wrapper c=new Wrapper();
				c.get_array_list().add(current);
				this.array[(int)ipstart_hash]=c;
			}
			else
			{
				this.array[(int)ipstart_hash].get_array_list().add(current);
			}
		}
		for(int i=0;i<array_length;i++)
		{
			if(this.array[i]!=null)
			{
				Collections.sort(this.array[i].get_array_list());
			}
		}
	}
	//-------------------------------------------------------------------------------------------------------
	public Ip_start_end_property get_mach_for_ip(long a_ipstart) throws UnparseableIpv4Exception
	{
		long ip_hash=a_ipstart >>> (32-this.bits_used_as_hash);
		
		for(long current_array_index=ip_hash;current_array_index>=0;current_array_index--)
		{
			if(this.array[(int)current_array_index]==null)
			{
				//need to keep downwards, since we could be in this scenario:
				//B|---------------------|
				//			A|-----------------|
			}
			else
			{
				for(int current_arraylist_index=0;current_arraylist_index<this.array[(int)current_array_index].get_array_list().size();current_arraylist_index++)
				{
					Ip_start_end_property current_node=this.array[(int)current_array_index].get_array_list().get(current_arraylist_index);
					if(current_node.get_double_a()<=a_ipstart && a_ipstart<current_node.get_double_b())
					{
						return current_node;
					}
					else if(current_node.get_double_a()>a_ipstart)
					{
						return new Ip_start_end_property(null, null, null);
					}
				}
			}
		}
		return new Ip_start_end_property(null, null, null);
	}
	//-------------------------------------------------------------------------------------------------------
	
}
