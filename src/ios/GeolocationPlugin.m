//
//  GeolocationPlugin.m
//  Geolocation
//
//  Created by dev on 2/9/16.
//
//

#import "GeolocationPlugin.h"

@implementation GeolocationPlugin

-(void)start:(CDVInvokedUrlCommand *)command
{
    coord1 = [command.arguments objectAtIndex:0];   // Get center and radio of region1
    coord2 = [command.arguments objectAtIndex:1];   // Get center and radio of region2
    url = [command.arguments objectAtIndex:2];      // Get server url

    
    [self stopTimer];
    [self startTimedTask];
}

-(void)startTimedTask{
    time_interval = [NSTimer scheduledTimerWithTimeInterval:75.0 target:self selector:@selector(performBackgroundTask) userInfo:nil repeats:YES];
}
-(void)performBackgroundTask{
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        dispatch_async(dispatch_get_main_queue(), ^{
            [self initData];
        });
    });
}

-(void)stopTimer{
    if(time_interval)
    {
        [time_interval invalidate];
        time_interval = nil;
        [self stopUpdatingLocationWithMessage:NSLocalizedString(@"Error", @"Error")];
    }
}

-(void)stop:(CDVInvokedUrlCommand *)command
{
    [self stopTimer];
}

-(void)initData{
    [self createDataBase];
    
    // Add location Manager class
    if (nil == self.locationManager) {
        self.locationManager = [[CLLocationManager alloc] init];
        // Get version of iOS
        if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 8.0) {
            
            [ self.locationManager requestWhenInUseAuthorization];
            
            [self.locationManager requestAlwaysAuthorization];
        }
    }
    now     = [NSDate date];
    now1    = [NSDate date];
    
    if (nil == self.locationManager)
        self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    
    
    [self.locationManager startMonitoringSignificantLocationChanges];
    [self.locationManager startUpdatingLocation];
    
    // add multi region
    [self regionStart:coord1];
    [self regionStart:coord2];
}


// recieve GPS updated location
- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations {
    
    location = [locations lastObject];
    
    NSDate* timestamp = location.timestamp;
    if (fabs([now timeIntervalSinceDate:timestamp]) < 60){
    }
    else{
        NSLog(@"Time 60 more than print");
        [self makeJSON_Make ];
        [self stopUpdatingLocationWithMessage:NSLocalizedString(@"Error", @"Error")];
    }
}

- (void)stopUpdatingLocationWithMessage:(NSString *)state {
    
    [self.locationManager stopUpdatingLocation];
    self.locationManager.delegate = nil;
}

-(void)regionStart:(NSString*)str_array{
    NSArray *ns_coord = [str_array componentsSeparatedByString:@","];
    NSArray *array_coord = [ns_coord[0] componentsSeparatedByString:@":"];
    NSString *identfier = [NSString stringWithFormat:@"%@", array_coord[1]];
    
    
    NSArray *str_lat_array = [ns_coord[1] componentsSeparatedByString:@":"];
    NSString *str_lat = [NSString stringWithFormat:@"%@", str_lat_array[1]];
    float lat = [str_lat floatValue];
    
    NSArray *str_lon_array = [ns_coord[2] componentsSeparatedByString:@":"];
    NSString *str_lon = [NSString stringWithFormat:@"%@", str_lon_array[1]];
    float lon = [str_lon floatValue];
    
    NSArray *str_radio_array = [ns_coord[3] componentsSeparatedByString:@":"];
    NSString *str_radio = [NSString stringWithFormat:@"%@", str_radio_array[1]];
    float radio = [str_radio floatValue];
    
    
    CLLocationCoordinate2D center = CLLocationCoordinate2DMake(lat, lon);
    CLRegion *region = [[CLCircularRegion alloc] initWithCenter:center radius:radio identifier:identfier];
    
    [self.locationManager startMonitoringForRegion:region];
     self.locationManager.pausesLocationUpdatesAutomatically = YES ;

}

-(void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region{

}
-(void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region{
    DateFormatter = [[NSDateFormatter alloc] init];
    [DateFormatter setDateFormat:@"yyyyMMdd'T'HH:mm:ss"];
    NSString *time = [DateFormatter stringFromDate:manager.location.timestamp];
    
    float lat = manager.location.coordinate.latitude;
    float lon = manager.location.coordinate.longitude;
    float alt = manager.location.altitude;
    NSString* identfier = region.identifier;
    [self insertData:lat lon:lon alt:alt currentTime:time identfier:identfier region:@"YES"];
}
-(void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region{
    DateFormatter = [[NSDateFormatter alloc] init];
    [DateFormatter setDateFormat:@"yyyyMMdd'T'HH:mm:ss"];
    NSString *time = [DateFormatter stringFromDate:manager.location.timestamp];
    
    float lat = manager.location.coordinate.latitude;
    float lon = manager.location.coordinate.longitude;
    float alt = manager.location.altitude;
    NSString* identfier = region.identifier;
    [self insertData:lat lon:lon alt:alt currentTime:time identfier:identfier region:@"NO"];
}

-(void)createDataBase{
    
    //Get the documents directory
    NSArray *dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docsDir = [dirPaths objectAtIndex:0];
    
    //Build the path to the database file
    dataBasePath = [[NSString alloc]initWithString:[docsDir stringByAppendingPathComponent:@"GPSTrack.sqlite"]];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    
    
    if ([fileManager fileExistsAtPath:dataBasePath] == NO) {
        const char *dbpath = [dataBasePath UTF8String];
        
        if (sqlite3_open(dbpath, &contactDB) == SQLITE_OK) {
            char *errMsg;
            
            const char *sql_table = "CREATE TABLE GPSTable(_id INTEGER PRIMARY KEY NOT NULL, latitude TEXT, longitude TEXT, altitude TEXT, timerInterval TEXT,  identfier  TEXT, region TEXT);";
            
            if (sqlite3_exec(contactDB, sql_table, NULL, NULL, &errMsg) != SQLITE_OK) {
                NSLog(@"Failed to create table");
            }
            sqlite3_close(contactDB);
        }
        else{
            NSLog(@"Failed to open/create databese");
        }
    }
}

-(void)insertData:(float)lat lon:(float)lon alt:(float)alt currentTime:(NSString*)currentTime identfier:(NSString*)identfier region:(NSString*)region {
    sqlite3_stmt    *statement;
    
    const char *dbpath = [dataBasePath UTF8String];
    
    if (sqlite3_open(dbpath, &contactDB) == SQLITE_OK)
    {
        //NSString *insertSQL = [NSString stringWithFormat:@"INSERT INTO GPSTable values(null, '%+.6f', '%.6f', '%.6f', '%@','%@', '%@')", location.coordinate.latitude, location.coordinate.longitude, location.altitude, DateFormatter, identfier,region];
        
        
        NSString *insertSQL = [NSString stringWithFormat:@"INSERT INTO GPSTable values(null, '%.6f', '%.6f', '%.6f', '%@','%@', '%@')", lat, lon, alt, currentTime, identfier, region];

        const char *insert_stmt = [insertSQL UTF8String];
        
        sqlite3_prepare_v2(contactDB, insert_stmt, -1, &statement, NULL);
        if (sqlite3_step(statement) == SQLITE_DONE)
        {
            NSLog(@"Success to add contact");
        } else {
            NSLog(@"Failed to add contact");
        }
        sqlite3_finalize(statement);
        sqlite3_close(contactDB);
    }
}
-(void)makeJSON_Make{
    NSMutableArray *json_data = [[NSMutableArray alloc] init];
    if (sqlite3_open([dataBasePath UTF8String], &contactDB) == SQLITE_OK) {
        NSString *sql_makeJOSN = [NSString stringWithFormat:@"SELECT * FROM GPSTable"];
        sqlite3_stmt *statement;
        
        if (sqlite3_prepare_v2(contactDB, [sql_makeJOSN UTF8String], -1, &statement, NULL)==SQLITE_OK) {
            if (sqlite3_step(statement) == SQLITE_ROW) {
                
                while (sqlite3_step(statement) == SQLITE_ROW) {
                    
                    //NSLog(@"{timestamp: '%s', source: \"iOS\", gps: {latitude: %s, longitude: %s, altitude: %s, timerInterval %s}, region: {\"identifier\": \"%s\"}}", sqlite3_column_text(statement, 4),  sqlite3_column_text(statement, 1), sqlite3_column_text(statement, 2), sqlite3_column_text(statement, 3), sqlite3_column_text(statement, 4), sqlite3_column_text(statement, 5), sqlite3_column_text(statement, 6));
                    
                    
                    JSONString = [NSString stringWithFormat:@"{timestamp: '%s', source: \"iOS\", gps: {latitude: %s, longitude: %s, altitude: %s, timerInterval %s}, region: {\"%s\": \"%s\"}},", sqlite3_column_text(statement, 4),  sqlite3_column_text(statement, 1), sqlite3_column_text(statement, 2), sqlite3_column_text(statement, 3), sqlite3_column_text(statement, 4), sqlite3_column_text(statement, 5), sqlite3_column_text(statement, 6)];
                    [json_data addObject:JSONString];
                    
                }
                sqlite3_finalize(statement);
                sqlite3_close(contactDB);
                
                [self postJSON:json_data];
                
            }
        }
    }
}

-(void)postJSON:(NSMutableArray*)jdata
{
    NSString *send_jData;
    send_jData = [NSString stringWithFormat:@"{\"choice\" : [\"scan\"], \"data\" : {%@}, \"comment\" : \"test\", \"poll\" : \"/api/v1/polls/poll/rfscan/\"}", jdata];
    
    //create a NSURL object from the string data
    NSURL *myUrl = [NSURL URLWithString:url];
    
    //create a mutable HTTP request
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:myUrl];
    
    
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request setValue:@"charset=utf-8" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:[send_jData dataUsingEncoding:NSUTF8StringEncoding]];
    
    NSError *errorReturned = nil;
    NSURLResponse *theResponse =[[NSURLResponse alloc]init];
    [NSURLConnection sendSynchronousRequest:request returningResponse:&theResponse error:&errorReturned];
    
    if (errorReturned) {
        // Handle error.
        NSLog(@"Handle error!");
    }
    else
    {
        if (sqlite3_open([dataBasePath UTF8String], &contactDB) == SQLITE_OK) {
            NSString *query_delete=[NSString stringWithFormat:@"DELETE FROM GPSTable WHERE 1"];
            sqlite3_stmt *statement;
            if (sqlite3_prepare_v2(contactDB, [query_delete UTF8String], -1, &statement, NULL)==SQLITE_OK) {
                NSLog(@"Delete Query Success");
            }
        }
    }
}
@end
