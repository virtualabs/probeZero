$fn=200;

module MainCap(w, l, h){
	/* Main case */
	difference() {
		hull(){
			translate([3, 3, 0]) cylinder(r=3, h=h);
			translate([w-3, 3, 0]) cylinder(r=3, h=h);
			translate([w-3, l-3, 0]) cylinder(r=3, h=h); 
			translate([3, l-3, 0]) cylinder(r=3, h=h);
		};
	
	/* nuts supports */
/*
	translate([3,3,2]) cylinder(r=4, h=h+0.5);
	translate([w-3,3,2]) cylinder(r=4, h=h+0.5);
	translate([w-3,l-3,2]) cylinder(r=4, h=h+0.5);
	translate([3,l-3,2]) cylinder(r=4, h=h+0.5);
*/
	}
}

module TopCase(w,l,h){
	difference(){
		MainCap(w,l,h);
		translate([4.5, (l-9)/2, -0.1]) cube([4, 9, 2.2]);
		translate([3,3,h-20.5]) cylinder(r=1.6, h=30);
		translate([w-3,3,h-20.5]) cylinder(r=1.6, h=30);
		translate([w-3,l-3,h-20.5]) cylinder(r=1.6, h=30);
		translate([3,l-3,h-20.5]) cylinder(r=1.6, h=30);
	}
}

TopCase(88, 35, 2);