package com.example.ryd

import android.os.Parcel
import android.os.Parcelable

data class Ride(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val fromLocation: String = "",
    val destination: String = "",
    val departureTime: Long = 0,
    val isDriver: Boolean = false,
    val seats: Int = 0,
    val description: String = "",
    val timestamp: Long = 0,
    val userDepartment: String = "",
    val userYear: String = "",
    var status: String = "upcoming"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "upcoming"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(userId)
        parcel.writeString(userName)
        parcel.writeString(userPhoto)
        parcel.writeString(fromLocation)
        parcel.writeString(destination)
        parcel.writeLong(departureTime)
        parcel.writeByte(if (isDriver) 1 else 0)
        parcel.writeInt(seats)
        parcel.writeString(description)
        parcel.writeLong(timestamp)
        parcel.writeString(userDepartment)
        parcel.writeString(userYear)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Ride> {
        override fun createFromParcel(parcel: Parcel): Ride {
            return Ride(parcel)
        }

        override fun newArray(size: Int): Array<Ride?> {
            return arrayOfNulls(size)
        }
    }
}