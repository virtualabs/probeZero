use <powerbank.scad>
use <raspzero.scad>

$fn=200;

/* Largeur de la carte boost. */
boost_width = 18.5;

/* Largeur du support boost. */
boost_support_width = boost_width + 6.0 - 0.8;

/* taille de grip derrière support boost. */
boost_support_grip_width = 2.0;

/* distance entre le support boost et le bord du case. */
boost_support_card_gap = 7.0;

boost_card_thick = 1.0;
boost_card_height = 22.0;
boost_support_height = 13.0 + boost_card_height;

module SupportBoost() {
	difference(){
		difference(){
			cube([
				boost_support_card_gap + boost_support_grip_width+boost_card_thick,
				boost_support_width,
				boost_support_height
			]);
		
			translate([-0.1, 3.0,-0.1])
				cube([
					boost_support_card_gap + boost_support_grip_width+boost_card_thick+0.2,
					boost_width-0.8,
					boost_support_height+0.2
				]);				
		}
		translate([
			boost_support_card_gap - boost_card_thick - 0.1,
			3.0-0.4,
			13.0
		]) cube([boost_card_thick+0.1,boost_width, boost_card_height+0.2]);
	}
}

module MainCase(w, l, h){
	/* Main case */
	difference(){
		hull(){
			translate([3, 3, 0]) cylinder(r=3, h=h);
			translate([w-3, 3, 0]) cylinder(r=3, h=h);
			translate([w-3, l-3, 0]) cylinder(r=3, h=h); 
			translate([3, l-3, 0]) cylinder(r=3, h=h);
		};
		translate ([0,0,2]) hull(){
			translate([5, 5, 0]) cylinder(r=3, h=h);
			translate([w-5, 5, 0]) cylinder(r=3, h=h);
			translate([w-5, l-5, 0]) cylinder(r=3, h=h); 
			translate([5, l-5, 0]) cylinder(r=3, h=h);
		};
	 }

	/* nuts supports */
	translate([3,3,0.5]) cylinder(r=3, h=h-0.5);
	translate([w-3,3,0.5]) cylinder(r=3, h=h-0.5);
	translate([w-3,l-3,0.5]) cylinder(r=3, h=h-0.5);
	translate([3,l-3,0.5]) cylinder(r=3, h=h-0.5);
}

module BottomCase(w,l,h) {
	difference(){
		MainCase(w,l,h);
		translate([3,3,h-20.5]) cylinder(r=1.2, h=30);
		translate([w-3,3,h-20.5]) cylinder(r=1.2, h=30);
		translate([w-3,l-3,h-20.5]) cylinder(r=1.2, h=30);
		translate([3,l-3,h-20.5]) cylinder(r=1.2, h=30);
		translate([-0.1, (35-boost_support_width)/2+9.5+2.6, 13+6.5])
			rotate([0,90,0]) cylinder(r=1.5, h=10);
	}

	/* Add a support for the dc/dc boost card */
	translate([1.99, (35-boost_support_width)/2, 0.0])
		SupportBoost();
	
	/*
	difference(){
		union(){
			translate([30, 0.5, 0.5]) cube([2, 36, 10]);
			translate([90, 0.5, 0.5]) cube([2, 36, 10]);
		}
		translate([13, 19, 19.5]) rotate([0,90,0]) PowerBank5V();
	}
	
	translate([0.5,10,0.5]) cube([w-100-8, 2, 9.5]);
	translate([0.5,28,0.5]) cube([w-100-8, 2, 9.5]);
	translate([0.5, 36, 0.5]) cube([w-0.5, 2, 19.5]);
	*/
}
BottomCase(88, 35, 35);
//SupportBoost();
//translate([12, 19.5, 19.5]) rotate([0,90,0]) PowerBank2200();
//translate([-20,-15,15]) rotate([0,-90,0])  RaspZero();
