-- feel free to replace geoip with the database name you want, as well as the ENGINE.
DROP DATABASE IF EXISTS geoip;
CREATE DATABASE geoip;
USE geoip;
CREATE TABLE IF NOT EXISTS TGIP_000_PROCESS_LOG 
(
  deliveryDate varchar(10) DEFAULT NULL,
  type_of_entry varchar(100) DEFAULT NULL,
  description varchar(100) DEFAULT NULL,
  num_rows int(11) DEFAULT NULL,
  target varchar(100) DEFAULT NULL,
  process_begin datetime DEFAULT NULL,
  process_end datetime DEFAULT NULL,
  length time DEFAULT NULL,
  notes varchar(500) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_100_LOAD_REGION 
(
  countryISO varchar(100) DEFAULT NULL,
  id varchar(100) DEFAULT NULL,
  region varchar(100) DEFAULT NULL,
  KEY IGIP_100_ID_COUNTRYISO (id,countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_101_LOAD_GEOIP_COUNTRY_WHOIS 
(
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) DEFAULT NULL,
  KEY IGIP_101_IPINTSTART (ipIntStart),
  KEY IGIP_101_IPINTEND (ipIntEnd),
  KEY IGIP_101_COUNTRYISO (countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_102_LOAD_GEOIP_ASNUM 
(
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  KEY IGIP_102_IPINTSTART (ipIntStart),
  KEY IGIP_102_IPINTEND (ipIntEnd),
  KEY IGIP_102_IPINTSTART_IPINTEND (ipIntStart,ipIntEnd)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_103_LOAD_GEOLITE_CITY_BLOCKS 
(
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  locId varchar(10) NOT NULL,
  KEY IGIP_103_IPINTSTART (ipIntStart),
  KEY IGIP_103_IPINTEND (ipIntEnd),
  KEY IGIP_103_LOCID (locId)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_104_LOAD_GEOLITE_CITY_LOCATIONS 
(
  locId varchar(10) NOT NULL,
  countryISO varchar(10) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  PRIMARY KEY (locId)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_200_BLOCKS_LOCATIONS 
(
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  KEY IGIP_200_IPINTSTART (ipIntStart),
  KEY IGIP_200_IPINTEND (ipIntEnd),
  KEY IGIP_200_COUNTRYISO_REGIONID (countryISO,regionId)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_201_BLOCKS_LOCATIONS_REGIONS 
(
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  KEY IGIP_201_IPINTSTART (ipIntStart),
  KEY IGIP_201_IPINTEND (ipIntEnd),
  KEY IGIP_201_COUNTRYISO (countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY 
(
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) DEFAULT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  KEY IGIP_202_IPINTSTART (ipIntStart),
  KEY IGIP_202_IPINTEND (ipIntEnd),
  KEY IGIP_202_COUNTRYISO (countryISO),
  KEY IGIP_202_IPINTSTART_IPINTEND (ipIntStart,ipIntEnd),
  KEY IGIP_202_REGION (region),
  KEY IGIP_202_COUNTRYISO_REGION (countryISO,region)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 
(
  ipIntStart int(10) unsigned NOT NULL DEFAULT '0',
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  ipStartB varchar(15) DEFAULT NULL,
  ipEndB varchar(15) DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) NOT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  KEY IGIP_203_IPINTSTART (ipIntStart),
  KEY IGIP_203_IPINTEND (ipIntEnd),
  KEY IGIP_203_COUNTRYISO (countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 
(
  ipIntStart int(10) unsigned NOT NULL DEFAULT '0',
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  ipStartB varchar(15) DEFAULT NULL,
  ipEndB varchar(15) DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  reason varchar(30) DEFAULT NULL,
  parent_start varchar(30) DEFAULT NULL,
  parent_end varchar(30) DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) NOT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  iteration int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (ipIntStart),
  KEY ipIntEnd (ipIntEnd),
  KEY countryISO (countryISO),
  KEY IGIP_204_IPINTSTART (ipIntStart),
  KEY IGIP_204_IPINTEND (ipIntEnd),
  KEY IGIP_204_COUNTRYISO (countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_205_PRE_ORPHANS 
(
  orphan_id int(11) NOT NULL AUTO_INCREMENT,
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  ipStartB varchar(15) DEFAULT NULL,
  ipEndB varchar(15) DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  reason varchar(30) DEFAULT NULL,
  parent_start int(10) unsigned DEFAULT NULL,
  parent_end int(10) unsigned DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) NOT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  PRIMARY KEY (orphan_id),
  KEY IGIP_205_IPINTSTART (ipIntStart),
  KEY IGIP_205_IPINTEND (ipIntEnd),
  KEY IGIP_205_COUNTRYISO (countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_206_PRE_ORPHANS_PREVIOUS_STEP 
(
  orphan_id int(11) NOT NULL AUTO_INCREMENT,
  ipIntStart int(10) unsigned DEFAULT NULL,
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  ipStartB varchar(15) DEFAULT NULL,
  ipEndB varchar(15) DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  reason varchar(30) DEFAULT NULL,
  parent_start int(10) unsigned DEFAULT NULL,
  parent_end int(10) unsigned DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) NOT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  PRIMARY KEY (orphan_id),
  KEY IGIP_206_IPINTSTART (ipIntStart),
  KEY IGIP_206_IPINTEND (ipIntEnd),
  KEY IGIP_206_COUNTRYISO (countryISO)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_300_RANGE_A_RANGES_B 
(
  ipIntStartA int(10) unsigned DEFAULT NULL,
  ipIntEndA int(10) unsigned DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  KEY IGIP_300_IPINTSTARTA (ipIntStartA),
  KEY IGIP_300_IPINTENDA (ipIntEndA),
  KEY IGIP_300_IPINTSTARTB (ipIntStartB),
  KEY IGIP_300_IPINTENDB (ipIntEndB),
  KEY IGIP_300_IPINTSTARTA_IPINTENDA (ipIntStartA,ipIntEndA)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_301_NEW_INVALID_RECORDS 
(
  ipIntStart int(10) unsigned NOT NULL,
  KEY IGIP_301_IPINTSTART (ipIntStart)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_400_LAST_BOARD 
(
  data_timestamp date NOT NULL,
  ip_range varchar(33) NOT NULL,
  ipIntStart int(10) unsigned NOT NULL DEFAULT '0',
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  ipStartB varchar(15) DEFAULT NULL,
  ipEndB varchar(15) DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  reason varchar(30) DEFAULT NULL,
  parent_start varchar(30) DEFAULT NULL,
  parent_end varchar(30) DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) NOT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  iteration int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (ipIntStart),
  KEY IGIP_400_IPINTSTART (ipIntStart),
  KEY IGIP_400_IPINTEND (ipIntEnd),
  KEY IGIP_400_COUNTRYISO (countryISO),
  KEY IGIP_400_ASNUMBER (ASNumber)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS TGIP_401_HISTORIC_BOARD 
(
  data_valid_from date NOT NULL,
  data_valid_to date DEFAULT NULL,
  data_timestamp date NOT NULL,
  ip_range varchar(33) NOT NULL,
  ipIntStart int(10) unsigned NOT NULL DEFAULT '0',
  ipIntEnd int(10) unsigned DEFAULT NULL,
  ipStart varchar(15) DEFAULT NULL,
  ipEnd varchar(15) DEFAULT NULL,
  ipIntStartB int(10) unsigned DEFAULT NULL,
  ipIntEndB int(10) unsigned DEFAULT NULL,
  ipStartB varchar(15) DEFAULT NULL,
  ipEndB varchar(15) DEFAULT NULL,
  ASNumber varchar(100) DEFAULT NULL,
  reason varchar(30) DEFAULT NULL,
  parent_start varchar(30) DEFAULT NULL,
  parent_end varchar(30) DEFAULT NULL,
  locId varchar(10) DEFAULT NULL,
  countryISO varchar(10) DEFAULT NULL,
  country varchar(100) NOT NULL,
  region varchar(100) DEFAULT NULL,
  regionId varchar(100) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  postalCode varchar(10) DEFAULT NULL,
  latitude varchar(100) DEFAULT NULL,
  longitude varchar(100) DEFAULT NULL,
  metroCode varchar(100) DEFAULT NULL,
  areaCode varchar(100) DEFAULT NULL,
  iteration int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (data_valid_from,ipIntStart),
  KEY IGIP_401_IPINTSTART (ipIntStart),
  KEY IGIP_401_IPINTEND (ipIntEnd),
  KEY IGIP_401_COUNTRYISO (countryISO),
  KEY IGIP_401_ASNUMBER (ASNumber),
  KEY IGIP_401_DATA_VALID_TO (data_valid_to),
  KEY IGIP_401_DATA_TIMESTAMP (data_timestamp)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
