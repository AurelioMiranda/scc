package tukano.impl.data;

import java.util.Objects;
import java.util.Random;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Following{

	@Id 
	String follower;
	
	@Id 
	String followee;

	private String id;

	Following() {}

	public Following(String follower, String followee) {
		super();
		this.follower = follower;
		this.followee = followee;

		Random rand = new Random();
		int n = rand.nextInt(1000);
		this.id = followee + follower + n;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFollower() {
		return follower;
	}

	public void setFollower(String follower) {
		this.follower = follower;
	}

	public String getFollowee() {
		return followee;
	}

	public void setFollowee(String followee) {
		this.followee = followee;
	}

	@Override
	public int hashCode() {
		return Objects.hash(followee, follower);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Following other = (Following) obj;
		return Objects.equals(followee, other.followee) && Objects.equals(follower, other.follower);
	}

	@Override
	public String toString() {
		return "Following [follower=" + follower + ", followee=" + followee + "]";
	}
	
	
}