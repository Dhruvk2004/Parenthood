# ParentHood – Your Partner in Child Digital Safety

![Logo](https://raw.githubusercontent.com/Dhruvk2004/Parenthood/master/app/src/main/res/drawable/logo.png)

**ParentHood** is a mobile application designed to help parents monitor and ensure their children's digital safety. 

---

## 🚀 Features

- 📍 **Real-Time Location Tracking** – Track your child's current location with frequent updates.
- 🧭 **Geofence Zones** – Add custom geofences to monitor safe/unsafe areas with distance-based alerts.
- 📲 **App Activity Notifications** – Get notified when apps are installed or deleted on the child’s device.
- ⏱️ **App Screen Time Limits** – Set daily usage limits for specific apps and restrict overuse.
- 🔐 **QR Code Based Connection** – Secure and quick linking between parent and child devices.

---

## 🛠️ Technologies Used

- **Language**: Kotlin  
- **UI Framework**: Android XML & Material Design  
- **Backend**: Firebase  
  - Firebase Authentication  
  - Cloud Firestore  
- **Libraries & APIs**:
  - Google Maps SDK
  - Geocoding & Places API

---

## 📱 How It Works

1. **QR-Based Connection**  
   Parent scans the QR code from the child’s device to securely establish a connection.
2. **Live Location Tracking**  
   The app continuously pushes the child's location to Firestore for the parent to access.
3. **Geofence Monitoring**  
   Parents can draw a virtual circle on the map with a custom radius to define safe zones.
4. **App Monitoring**  
   Notifications are triggered on app install/uninstall events from the child’s device.
5. **Screen Time Limiting**  
   Set per-app or overall screen time limits; overuse restricts app access and notifies the parent.

---

## 📦 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Dhruvk2004/Parenthood.git
---

## 🔮 Future Plans

- 📊 **Parental Insights Dashboard** with detailed app usage analytics.
- 🔎 **Social Media Monitoring** features to help understand digital behavior patterns.

---

## 🤝 Contributions

Contributions are welcome!  
Whether it's fixing bugs, improving documentation, or proposing new features — feel free to fork this repo and submit a pull request.

---

## 📄 License

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.
