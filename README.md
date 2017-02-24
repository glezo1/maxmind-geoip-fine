# maxmind_fine
ETL over maxmind geoip files

The (awesome) guys in www.maxmind.com deliver a completely free dataset of ip geolocation. 
The information is quite complete, and reliable enough.

Yet, I kinda dislike the format they use to deliver the info.

The system itself is a multi-dimensional star scheme, having per dimensions:

  -AS Number
  
  -Geography (country, region,etc)
  
and being the fact table merely 

  the begin and the end of the range whose ASNumber is X and its geography Y.
  
  
So, the guys in maxmind distribute several csv files, one per each denormalized-branch of the star, it is:

file1 (ASNumber dimension)

1.0.0.0 - 1.0.0.255 ASXXXXX

1.0.1.0 - 1.0.1.255 ASYYYYY


file2 (geography dimension)

1.0.0.0 - 1.0.1.255 Spain, Madrid


This is nice, but what if you need all the ips of the ASXXXXX AND Spain?

This ETL resolves the several-denormalized-branch of the star scheme into a single denormalized-scheme table.

Probably, one of the most insidious ETLs ever made by the human hand. Trust me, you might want not to read the code at all.

It might take ~15 minutes to execute on a standard laptop, and might demand A LOT of memory.


Enjoy it!

(I would've liked to deliver the single denormalized-scheme table instead of the ETL that generates it, but I'm not able to according to the maxmind terms and conditions).

