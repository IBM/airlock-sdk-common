import Foundation

/**
* Automatically generated file by Airlock server. DO NOT MODIFY
*/
@objc public class AirlockConstants : NSObject {
	@objc static let PushConfiguration = PushConfiguration_impl()

	@objc class PushConfiguration_impl : NSObject {
		let PUSH_NOTIFICATIONS="PushConfiguration.Push Notifications"
	}
	@objc static let Forecasts = Forecasts_impl()

	@objc class Forecasts_impl : NSObject {
		let FORECAST15DAYS="Forecasts.Forecast15Days"
		let FORECAST48HOURS="Forecasts.Forecast48Hours"
	}
	@objc static let AdFree = AdFree_impl()

	@objc class AdFree_impl : NSObject {
		let ADFREE="AdFree.AdFree"
	}
	@objc static let test = test_impl()

	@objc class test_impl : NSObject {
		let F1="test.F1"
	}
	@objc static let elik = elik_impl()

	@objc class elik_impl : NSObject {
		let TEST="elik.test"
	}
	@objc static let NewRelic = NewRelic_impl()

	@objc class NewRelic_impl : NSObject {
		let NEWRELIC="NewRelic.NewRelic"
	}
	@objc static let Layer = Layer_impl()

	@objc class Layer_impl : NSObject {
		let HIDEDURINGANIMATION="Layer.HideDuringAnimation"
	}
	@objc static let Ratings = Ratings_impl()

	@objc class Ratings_impl : NSObject {
		let SMART_RATINGS="Ratings.Smart Ratings"
	}
	@objc static let APIKeys = APIKeys_impl()

	@objc class APIKeys_impl : NSObject {
		let SUN_API_KEY="APIKeys.Sun API Key"
	}
	@objc static let Ads = Ads_impl()

	@objc class Ads_impl : NSObject {
		let SLOT_CONFIGURATION="Ads.Slot Configuration"
		let REFRESH_TIMES="Ads.Refresh Times"
		let ADS_CONFIGURATION="Ads.Ads Configuration"
	}
	@objc static let ClearMapboxCache = ClearMapboxCache_impl()

	@objc class ClearMapboxCache_impl : NSObject {
		let CLEARMAPBOXCACHE="ClearMapboxCache.ClearMapboxCache"
	}
	@objc static let Maps = Maps_impl()

	@objc class Maps_impl : NSObject {
		let STYLES="Maps.Styles"
		let MAPS="Maps.Maps"
	}
	@objc static let sensors = sensors_impl()

	@objc class sensors_impl : NSObject {
		let LBS_SENSOR="sensors.LBS Sensor"
		let BAROMETRIC_PRESSURE_SENSOR="sensors.Barometric Pressure Sensor"
		let REPORTER="sensors.Reporter"
		let SENSOR_SYSTEM="sensors.Sensor System"
	}
	@objc static let Environment = Environment_impl()

	@objc class Environment_impl : NSObject {
		let PANGEASETTINGS="Environment.PangeaSettings"
	}
	@objc static let Layers = Layers_impl()

	@objc class Layers_impl : NSObject {
		let HD_SATELLITE="Layers.HD Satellite"
		let STORMREPORTS="Layers.StormReports"
		let US_ONLY="Layers.US Only"
		let WINTER_WEATHER="Layers.Winter Weather"
		let LOCAL_STORM_REPORTS="Layers.Local Storm Reports"
		let LAYER_CONFIGURATION="Layers.Layer Configuration"
	}
	@objc static let GDPR = GDPR_impl()

	@objc class GDPR_impl : NSObject {
		let PRIVACY="GDPR.Privacy"
	}
}
