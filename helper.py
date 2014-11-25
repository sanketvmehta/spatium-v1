import sys
import db


def create_table(k, cursor):
    """create table for storing co-locations and instances of given size"""
    table_candidate_name = "candidate"+str(k)
    table_instance_name = "instance"+str(k)

    table_instance_sql = "CREATE TABLE IF NOT EXISTS "
    table_instance_sql+="`"+table_instance_name+"` (`label` int(11) NOT NULL,"
    for i in range (0,k):
        table_instance_sql+="`instanceid"+str(i+1)+"` int(11) NOT NULL,"
    table_instance_sql += "KEY `label` (`label`) ) "
    db.add_table(table_instance_sql, cursor)
    
    table_candidate_sql = "CREATE TABLE IF NOT EXISTS "
    table_candidate_sql+="`"+table_candidate_name+"` (`label` int(11) NOT NULL,"
    for i in range (0,k):
        table_instance_sql+="`typeid"+str(i+1)+"` int(11) NOT NULL,"    
    table_candidate_sql += "`pi` double NOT NULL, `labelprev1` int(11) NOT NULL, `labelprev2` int(11) NOT NULL, KEY `label` (`label`) ) "
    db.add_table(table_candidate_sql, cursor)
